package com.backend.orbitflow.domain.avatar.listener;

import com.backend.orbitflow.domain.point.service.PointLedger;
import com.backend.orbitflow.domain.user.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AvatarEventListener {

    private final PointLedger pointLedger;

    // 가입과 같은 트랜잭션에서 기본 아바타와 초기 포인트 지급 (아바타 없는 사용자가 생기지 않도록 동기 처리)
    @EventListener
    public void handle(UserRegisteredEvent event) {
        pointLedger.createAvatar(event.user());
    }
}
