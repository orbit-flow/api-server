package com.backend.orbitflow.domain.auth.dto.response;

import com.backend.orbitflow.global.common.dto.response.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthSuccessCode implements SuccessCode {

    DORMANT_RELEASE(HttpStatus.OK, "휴면 상태가 해제되었습니다. 다시 로그인해주세요."),
    PASSWORD_RESET_MAIL_SENT(HttpStatus.OK, "가입된 이메일이라면 비밀번호 재설정 링크가 발송됩니다."),
    PASSWORD_RESET_SUCCESS(HttpStatus.OK, "비밀번호가 재설정되었습니다. 새 비밀번호로 로그인해주세요."),
    LOGIN_SUCCESS(HttpStatus.OK, "로그인 되었습니다."),
    LOGOUT_SUCCESS(HttpStatus.OK, "로그아웃 되었습니다."),
    OAUTH_LOGIN_SUCCESS(HttpStatus.OK, "소셜 로그인 되었습니다."),
    REISSUE_SUCCESS(HttpStatus.OK, "토큰이 재발급 되었습니다."),
    EMAIL_CODE_SEND(HttpStatus.OK, "인증 코드가 발급되었습니다."),
    EMAIL_VARIFY_SUCCESS(HttpStatus.OK, "이메일 인증이 완료되었습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
