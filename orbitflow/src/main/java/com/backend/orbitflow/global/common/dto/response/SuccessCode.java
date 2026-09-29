package com.backend.orbitflow.global.common.dto.response;

import org.springframework.http.HttpStatus;

public interface SuccessCode {

    HttpStatus getHttpStatus();
    String getMessage();
}
