package com.backend.orbitflow.domain.user.error;

import org.springframework.http.HttpStatus;

import com.backend.orbitflow.global.common.error.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter 
@RequiredArgsConstructor 
public enum UserErrorCode implements ErrorCode{

    BAD_USER_ROLE(HttpStatus.BAD_REQUEST,
            "잘못된 사용자 권한입니다.",
            "https://orbitflow.com/errors/bad-user-role",
            "Bad User Role"),
    EMAIL_DUPLICATE(HttpStatus.BAD_REQUEST,
            "이미 가입된 이메일 주소입니다.",
            "https://orbitflow.com/errors/email-duplicate",
            "Email Duplicate"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND,
            "존재하지 않는 사용자입니다.",
            "https://orbitflow.com/errors/user-not-found",
            "User Not Found"),
    WRONG_USER_NAME(HttpStatus.BAD_REQUEST,
            "잘못된 사용자 이름입니다.",
            "https://orbitflow.com/errors/wrong-user-name",
            "Wrong User Name"),
    PASSWORD_ALREADY_SET(HttpStatus.CONFLICT,
            "이미 비밀번호가 설정된 계정입니다. 비밀번호 변경을 이용해 주세요.",
            "https://orbitflow.com/errors/password-already-set",
            "Password Already Set"),
    OAUTH_ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND,
            "연결되지 않은 소셜 계정입니다.",
            "https://orbitflow.com/errors/oauth-account-not-found",
            "OAuth Account Not Found"),
    LAST_LOGIN_METHOD(HttpStatus.BAD_REQUEST,
            "마지막 로그인 수단은 해제할 수 없습니다. 비밀번호를 먼저 설정해 주세요.",
            "https://orbitflow.com/errors/last-login-method",
            "Last Login Method"),
    TEAM_OWNER_CANNOT_WITHDRAW(HttpStatus.BAD_REQUEST,
            "소유한 팀이 있으면 탈퇴할 수 없습니다. 팀 소유자를 다른 구성원에게 넘기거나 팀을 삭제해 주세요.",
            "https://orbitflow.com/errors/team-owner-cannot-withdraw",
            "Team Owner Cannot Withdraw"),
    PASSWORD_REQUIRED(HttpStatus.BAD_REQUEST,
            "현재 비밀번호를 입력해 주세요.",
            "https://orbitflow.com/errors/password-required",
            "Password Required"),
    UN_VARIFIED_EMAIL(HttpStatus.BAD_REQUEST,
            "인증되지 않은 이메일 주소입니다.",
            "https://orbitflow.com/errors/un-varified-email",
            "Un Varified Email"),
    PASSWORD_NOT_SET(HttpStatus.BAD_REQUEST,
            "비밀번호가 설정되지 않은 소셜 계정입니다. 비밀번호 설정을 먼저 진행해 주세요.",
            "https://orbitflow.com/errors/password-not-set",
            "Password Not Set"),
    SAME_AS_CURRENT_PASSWORD(HttpStatus.BAD_REQUEST,
            "새 비밀번호가 현재 비밀번호와 같습니다.",
            "https://orbitflow.com/errors/same-as-current-password",
            "Same As Current Password");

    private final HttpStatus httpStatus;
    private final String message;
    private final String type;
    private final String title;
}
