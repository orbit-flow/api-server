package com.backend.orbitflow.domain.auth.facade;

import com.backend.orbitflow.domain.auth.advice.SuspendedAccountOAuth2Exception;
import com.backend.orbitflow.domain.avatar.service.AvatarService;
import com.backend.orbitflow.domain.suspension.service.SuspensionNoticeTicketService;
import com.backend.orbitflow.domain.suspension.service.SuspensionService;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.domain.user.enums.UserRole;
import com.backend.orbitflow.domain.user.oauth.OAuth2UserInfo;
import com.backend.orbitflow.domain.user.service.OAuthService;
import com.backend.orbitflow.domain.user.service.UserService;
import com.backend.orbitflow.global.common.error.exception.CommonException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OAuthFacade {

    private final OAuthService oAuthService;
    private final UserService userService;
    private final AvatarService avatarService;
    private final SuspensionService suspensionService;
    private final SuspensionNoticeTicketService suspensionNoticeTicketService;

    // 회원 조회·가입·연결과 상태 확인만 하나의 트랜잭션으로 (예외 시 롤백 후 그대로 전파), 제공자 사용자 정보 조회(외부 HTTP)는 OAuth2UserLoader 가 트랜잭션 밖에서 수행
    // 탈퇴 계정 이메일 해제(행 락) 이후 재확인이 동시 로그인·가입의 커밋을 보도록 READ_COMMITTED
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public OAuth2User loadOrRegister(OAuth2UserInfo oAuth2UserInfo) {
        Optional<User> user = oAuthService.findUser(oAuth2UserInfo.getProvider(), oAuth2UserInfo.getProviderId());
        User oAuthUser;
        try {
            // 처음 보는 소셜 계정은 가입 : 탈퇴한 계정이 점유한 이메일은 그 계정의 식별정보·소셜 연결을 지워 풀어 줌 (재가입 허용)
            oAuthUser = user.orElseGet(() -> {
                userService.releaseWithdrawnEmail(oAuth2UserInfo.getEmail()).ifPresent(oAuthService::unlinkAll);
                return userService.registerSocialUser(oAuth2UserInfo.getEmail(), oAuth2UserInfo.getName(), oAuth2UserInfo.getProfileUrl());
            });
        } catch (CommonException e) {
            // 같은 이메일로 가입된 계정이 있으면 500 대신 FE 로그인 화면으로 (OAuth2 실패 핸들러로 전달)
            throw new OAuth2AuthenticationException(new OAuth2Error("email_already_registered"), e.getMessage());
        }

        // 소셜 가입 시 기본 아바타·초기 포인트는 가입 트랜잭션에서 함께 생성
        if (user.isEmpty()) {
            avatarService.createAvatar(oAuthUser);
            oAuthService.link(oAuthUser, oAuth2UserInfo.getProvider(), oAuth2UserInfo.getProviderId());
        }
        // 탈퇴 유예 계정 복구, 정지·휴면 계정은 소셜 로그인도 거부 (OAuth2 실패 핸들러로 전달)
        try {
            userService.restoreIfWithdrawn(oAuthUser);
        } catch (CommonException e) {
            throw new OAuth2AuthenticationException(new OAuth2Error("account_withdrawn"), e.getMessage());
        }
        // 정지 계정은 토큰 없이 FE 정지 안내 화면으로 (안내 화면은 조회 키로 정지 사유·기간 조회)
        if (suspensionService.findActiveNotice(oAuthUser).isPresent()) {
            throw new SuspendedAccountOAuth2Exception(suspensionNoticeTicketService.issue(oAuthUser.getUuid()));
        }
        try {
            userService.validateNotDormant(oAuthUser);
        } catch (CommonException e) {
            throw new OAuth2AuthenticationException(new OAuth2Error("account_dormant"), e.getMessage());
        }
        userService.updateLastLoginAt(oAuthUser);

        Map<String, Object> attributes = Map.of("uuid", oAuthUser.getUuid());
        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority(UserRole.ROLE_USER.toString())),
                attributes,
                "uuid"
        );
    }
}
