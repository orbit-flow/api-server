package com.backend.orbitflow.domain.point.dto.response;

import com.backend.orbitflow.domain.point.entity.PointTransaction;
import com.backend.orbitflow.domain.point.enums.PointTransactionType;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

// amount : 적립은 양수, 차감·회수는 음수
public record PointTransactionResponse(
        Long id,
        PointTransactionType type,
        int amount,
        int balanceAfter,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {

    public static PointTransactionResponse from(PointTransaction transaction) {
        return new PointTransactionResponse(
                transaction.getId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getBalanceAfter(),
                transaction.getCreatedAt()
        );
    }
}
