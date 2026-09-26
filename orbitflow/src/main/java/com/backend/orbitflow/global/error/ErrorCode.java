package com.backend.orbitflow.global.error;

import com.backend.orbitflow.global.dto.response.ResponseCode;

public interface ErrorCode extends ResponseCode {

    String getType();
    String getTitle();
}
