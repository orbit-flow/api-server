package com.backend.orbitflow.global.common.error;

import com.backend.orbitflow.global.common.dto.response.SuccessCode;

public interface ErrorCode extends SuccessCode {

    String getType();
    String getTitle();
}
