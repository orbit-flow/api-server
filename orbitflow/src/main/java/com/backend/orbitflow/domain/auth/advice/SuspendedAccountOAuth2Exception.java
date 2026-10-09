package com.backend.orbitflow.domain.auth.advice;

import lombok.Getter;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

// 소셜 로그인 대상 계정이 정지 중 : 실패 핸들러가 정지 안내 조회 키(ticket)와 함께 FE 정지 안내 화면으로 이동
@Getter
public class SuspendedAccountOAuth2Exception extends OAuth2AuthenticationException {

    private final String ticket;

    public SuspendedAccountOAuth2Exception(String ticket) {
        super(new OAuth2Error("account_suspended"), "정지된 계정입니다.");
        this.ticket = ticket;
    }
}
