package com.backend.orbitflow.global.dto.response;

import org.springframework.http.HttpStatus;

public interface ResponseCode {

    HttpStatus getHttpStatus();
    String getMessage();
}
