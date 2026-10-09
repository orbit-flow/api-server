package com.backend.orbitflow.domain.user.event;

import com.backend.orbitflow.domain.user.entity.User;

// 사용자 가입 (이메일·소셜) : 가입 트랜잭션 안에서 동기 처리되어 실패 시 가입도 롤백
public record UserRegisteredEvent(User user) {
}
