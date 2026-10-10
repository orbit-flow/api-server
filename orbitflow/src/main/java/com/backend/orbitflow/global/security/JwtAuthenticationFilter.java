package com.backend.orbitflow.global.security;

import java.io.IOException;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.backend.orbitflow.domain.suspension.cache.SuspendedUserCache;
import com.backend.orbitflow.domain.suspension.error.SuspensionErrorCode;
import com.backend.orbitflow.global.common.dto.response.CommonResponse;
import com.backend.orbitflow.global.common.error.ErrorCode;
import com.backend.orbitflow.global.error.GlobalErrorCode;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.backend.orbitflow.domain.user.enums.UserRole;
import tools.jackson.databind.ObjectMapper;

@Slf4j 
@Component 
@RequiredArgsConstructor 
public class JwtAuthenticationFilter extends OncePerRequestFilter{

    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;
    private final SuspendedUserCache suspendedUserCache;

    // 인증이 필요 없는 공개 엔드포인트(SecurityConfig permitAll)는 만료·잘못된 Authorization 헤더가 있어도 막지 않음
    // 로그아웃은 인증이 필요하므로 제외
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.startsWith("/api/auth/")) {
            return !path.equals("/api/auth/logout");
        }
        return HttpMethod.POST.matches(request.getMethod()) && path.equals("/api/users");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException 
    {    
        String tokenValue = jwtProvider.getJwtFromHeader(request);

        if (tokenValue != null) {
            try {
                Claims info = jwtProvider.getUserInfoFromToken(tokenValue);
                // auth 클레임이 없는 토큰(refresh token)은 access token이 아님
                if (info.get(JwtProvider.AUTHORIZATION_KEY, String.class) == null) {
                    log.warn("access token이 아닌 JWT 토큰입니다.");
                    sendErrorResponse(request, response, GlobalErrorCode.INVALID_TOKEN);
                    return;
                }
                // 로그아웃 토큰 여부와 정지 여부를 한 번의 Redis 조회로 확인
                // 블랙리스트는 서명이 유효한 access token만 등록되므로 토큰 검증 이후에 확인해도 응답은 동일
                SuspendedUserCache.AccessStatus access = suspendedUserCache.checkAccess(tokenValue, info.getSubject());
                if (access.loggedOut()) {
                    log.warn("로그아웃된 토큰입니다.");
                    sendErrorResponse(request, response, GlobalErrorCode.EXPIRED_TOKEN);
                    return;
                }
                // 정지 계정은 이미 발급된 토큰으로도 접근 불가
                if (access.suspended()) {
                    sendErrorResponse(request, response, SuspensionErrorCode.ACCOUNT_SUSPENDED);
                    return;
                }
                setAuthentication(info);
            } catch (ExpiredJwtException e) {
                log.warn("만료된 JWT 토큰입니다.");
                sendErrorResponse(request, response, GlobalErrorCode.EXPIRED_TOKEN);
                return;
            } catch (JwtException e) {
                log.warn("유효하지 않은 JWT 토큰입니다. {}", e.getMessage());
                sendErrorResponse(request, response, GlobalErrorCode.INVALID_TOKEN);
                return;
            } catch (IllegalArgumentException e) {
                log.warn("JWT 클레임이 비어있습니다. {}", e.getMessage());
                sendErrorResponse(request, response, GlobalErrorCode.INVALID_TOKEN);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }


    private void setAuthentication(Claims claims) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        String uuid = claims.getSubject();
        String email = claims.get("email", String.class);
        UserRole role = UserRole.valueOf(claims.get(JwtProvider.AUTHORIZATION_KEY, String.class));

        AuthUser authUser = new AuthUser(uuid, email, role);
        Authentication authentication = new JwtAuthenticationToken(authUser);

        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    private void sendErrorResponse(HttpServletRequest request, HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        CommonResponse<Void> errorResponse = CommonResponse.fail(
            null, 
            errorCode, 
            request.getRequestURL().toString()
        );

        String result = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(result);
    }
    
}