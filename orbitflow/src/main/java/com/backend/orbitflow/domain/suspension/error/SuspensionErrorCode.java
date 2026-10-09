package com.backend.orbitflow.domain.suspension.error;

import com.backend.orbitflow.global.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SuspensionErrorCode implements ErrorCode {

    SUSPENSION_NOT_FOUND(HttpStatus.NOT_FOUND,
            "존재하지 않는 정지 정보입니다.",
            "https://orbitflow.com/errors/suspension-not-found",
            "Suspension Not Found"),
    ALREADY_SUSPENDED(HttpStatus.CONFLICT,
            "이미 정지 중인 사용자입니다.",
            "https://orbitflow.com/errors/already-suspended",
            "Already Suspended"),
    NOT_ACTIVE_SUSPENSION(HttpStatus.BAD_REQUEST,
            "진행 중인 정지만 수정하거나 해제할 수 있습니다.",
            "https://orbitflow.com/errors/not-active-suspension",
            "Not Active Suspension"),
    CANNOT_SUSPEND_ADMIN(HttpStatus.BAD_REQUEST,
            "관리자 계정은 정지할 수 없습니다.",
            "https://orbitflow.com/errors/cannot-suspend-admin",
            "Cannot Suspend Admin"),
    INVALID_EXPIRES_AT(HttpStatus.BAD_REQUEST,
            "정지 만료 시각은 현재 이후여야 합니다.",
            "https://orbitflow.com/errors/invalid-expires-at",
            "Invalid Expires At"),
    SUSPENSION_NOTICE_EXPIRED(HttpStatus.BAD_REQUEST,
            "정지 안내 확인 시간이 지났거나 정지가 해제되었습니다. 다시 로그인해 주세요.",
            "https://orbitflow.com/errors/suspension-notice-expired",
            "Suspension Notice Expired"),
    ACCOUNT_SUSPENDED(HttpStatus.FORBIDDEN,
            "정지된 계정입니다.",
            "https://orbitflow.com/errors/account-suspended",
            "Account Suspended");

    private final HttpStatus httpStatus;
    private final String message;
    private final String type;
    private final String title;
}
