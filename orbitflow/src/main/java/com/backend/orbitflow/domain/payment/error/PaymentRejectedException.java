package com.backend.orbitflow.domain.payment.error;

import com.backend.orbitflow.global.common.error.ErrorCode;
import com.backend.orbitflow.global.common.error.exception.CommonException;

// 주문을 실패 상태로 바꾼 변경은 커밋하면서 오류로 응답할 때 사용 (PaymentServiceImpl.confirm 의 @Transactional 이 롤백 대상에서 제외)
public class PaymentRejectedException extends CommonException {

    public PaymentRejectedException(ErrorCode errorCode, Object data) {
        super(errorCode, data);
    }
}
