package com.backend.orbitflow.domain.auth.advice;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OAuth2FailureHandler implements AuthenticationFailureHandler {

    @Value("${app.front-url}")
    private String frontUrl;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {
        if (exception instanceof SuspendedAccountOAuth2Exception suspended) {
            response.sendRedirect(UriComponentsBuilder.fromUriString(frontUrl)
                    .path("/suspended")
                    .queryParam("ticket", suspended.getTicket())
                    .build()
                    .encode()
                    .toUriString());
            return;
        }
        String error = exception instanceof OAuth2AuthenticationException oauth2Exception
                ? oauth2Exception.getError().getErrorCode()
                : "login_failed";
        response.sendRedirect(UriComponentsBuilder.fromUriString(frontUrl)
                .path("/login")
                .queryParam("error", error)
                .build()
                .encode()
                .toUriString());
    }
}
