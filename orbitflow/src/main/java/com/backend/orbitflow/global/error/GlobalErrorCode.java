package com.backend.orbitflow.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import com.backend.orbitflow.global.common.error.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum GlobalErrorCode implements ErrorCode{

    MAIL_SEND_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,
            "메일 발송 중 오류가 발생했습니다.",
            "https://orbitflow.com/errors/mail-send-error",
            "Mail Send Error"),
    FILE_UPLOAD_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,
            "파일 업로드 중 에러가 발생했습니다.",
            "https://orbitflow.com/errors/file-upload-error",
            "File Upload Error"),
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST,
            "형식에 맞지 않는 파일입니다.",
            "https://orbitflow.com/errors/invalid-file-type",
            "Invalid File Type"),
    FILE_SIZE_EXCEED(HttpStatus.BAD_REQUEST,
            "업로드 가능한 파일 크기는 10MB입니다.",
            "https://orbitflow.com/errors/file-size-exceed",
            "File Size Exceed"),
    FAILED_DELETE_IMAGE(HttpStatus.INTERNAL_SERVER_ERROR,
            "이미지 삭제에 실패하였습니다.",
            "https://orbitflow.com/errors/failed-delete-image",
            "Failed Delete Image"),
    INVALID_FILE_URL(HttpStatus.BAD_REQUEST,
            "잘못된 파일 주소 형식입니다.",
            "https://orbitflow.com/errors/invalid-file-url",
            "Invalid File URL"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED,
           "로그인 정보가 만료되었습니다.",
           "https://orbitflow.com/errors/expired-Token",
           "Expired Token"),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST,
            "입력값이 올바르지 않습니다.",
            "https://orbitflow.com/errors/invalid-input-value",
            "Invalid Input Value"),
    CONCURRENT_REQUEST_CONFLICT(HttpStatus.CONFLICT,
            "동시에 처리 중인 요청이 있습니다. 잠시 후 다시 시도해주세요.",
            "https://orbitflow.com/errors/concurrent-request-conflict",
            "Concurrent Request Conflict"),
    INVALID_TOKEN(HttpStatus.BAD_REQUEST,
                "잘못된 로그인 정보입니다.",
                "https://orbitflow.com/errors/invalid-token",
                "Invalid Token");
    
    private final HttpStatus httpStatus;
    private final String message;
    private final String type;
    private final String title;
}
