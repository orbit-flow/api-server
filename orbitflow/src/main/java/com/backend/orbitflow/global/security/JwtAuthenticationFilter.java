package com.backend.orbitflow.global.security;

import java.io.IOException;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.backend.orbitflow.domain.suspension.cache.SuspendedUserCache;
import com.backend.orbitflow.domain.suspension.error.SuspensionErrorCode;
import com.backend.orbitflow.global.common.dto.response.CommonResponse;
import com.backend.orbitflow.global.common.error.ErrorCode;
import com.backend.orbitflow.global.error.GlobalErrorCode;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
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
    private final StringRedisTemplate redisTemplate;
    private final SuspendedUserCache suspendedUserCache;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException 
    {    
        String tokenValue = jwtProvider.getJwtFromHeader(request);

        if (tokenValue != null) {
            String blacklisted = redisTemplate.opsForValue().get(tokenValue);
            if (!ObjectUtils.isEmpty(blacklisted)) {
                log.error("로그아웃된 토큰입니다.");
                sendErrorResponse(request, response, GlobalErrorCode.EXPIRED_TOKEN);
                return;
            }

            try {
                Claims info = jwtProvider.getUserInfoFromToken(tokenValue);
                // 정지 계정은 이미 발급된 토큰으로도 접근 불가
                if (suspendedUserCache.isSuspended(info.getSubject())) {
                    sendErrorResponse(request, response, SuspensionErrorCode.ACCOUNT_SUSPENDED);
                    return;
                }
                setAuthentication(info);
            } catch (SecurityException | MalformedJwtException e) {
                log.error("유효하지 않은 JWT 서명입니다.", e);
                sendErrorResponse(request, response, GlobalErrorCode.INVALID_TOKEN);
                return;
            } catch (ExpiredJwtException e) {
                log.error("만료된 JWT 토큰입니다.");
                sendErrorResponse(request, response, GlobalErrorCode.EXPIRED_TOKEN);
                return;
            } catch (UnsupportedJwtException e) {
                log.error("지원되지 않는 JWT 토큰입니다.", e);
                sendErrorResponse(request, response, GlobalErrorCode.INVALID_TOKEN);
                return;
            } catch (IllegalArgumentException e) {
                log.error("JWT 클레임이 비어있습니다.", e);
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