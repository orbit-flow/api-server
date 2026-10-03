package com.backend.orbitflow.domain.auth.advice;

import com.backend.orbitflow.domain.auth.service.TokenService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final TokenService tokenService;

    @Value("${app.front-url}")
    private String frontUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        ResponseCookie refreshToken = tokenService.createRefreshToken(authentication.getName());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshToken.toString());

        response.sendRedirect(frontUrl + "/");
    }
}
