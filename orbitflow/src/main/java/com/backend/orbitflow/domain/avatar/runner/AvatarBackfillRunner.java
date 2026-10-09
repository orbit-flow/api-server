package com.backend.orbitflow.domain.avatar.runner;

import com.backend.orbitflow.domain.avatar.repository.AvatarRepository;
import com.backend.orbitflow.domain.point.service.PointLedger;
import com.backend.orbitflow.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

// 아바타 기능 도입 이전 가입자 보정 : 서버 시작 시 아바타가 없는 사용자에게 기본 아바타·초기 포인트 지급
// 신규 가입자는 가입 트랜잭션에서 생성되므로 이후 실행에서는 대상이 없음
@Slf4j
@Component
@RequiredArgsConstructor
public class AvatarBackfillRunner implements ApplicationRunner {

    private final AvatarRepository avatarRepository;
    private final PointLedger pointLedger;

    @Override
    public void run(ApplicationArguments args) {
        List<User> users = avatarRepository.findUsersWithoutAvatar();
        if (users.isEmpty()) {
            return;
        }
        // 사용자별 개별 트랜잭션 (PointLedger.createAvatar)
        users.forEach(pointLedger::createAvatar);
        log.info("기존 사용자 아바타 생성 완료: {}건", users.size());
    }
}
