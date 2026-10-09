package com.backend.orbitflow.domain.auth.facade;

import com.backend.orbitflow.domain.auth.error.AuthErrorCode;
import com.backend.orbitflow.domain.suspension.service.SuspensionService;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.domain.user.enums.UserRole;
import com.backend.orbitflow.domain.user.oauth.*;
import com.backend.orbitflow.domain.user.service.OAuthService;
import com.backend.orbitflow.domain.user.service.UserService;
import com.backend.orbitflow.global.common.error.exception.CommonException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional
public class OAuthFacade extends DefaultOAuth2UserService {

    private final OAuthService oAuthService;
    private final UserService userService;
    private final SuspensionService suspensionService;

    @Override
    @NullMarked
    public OAuth2User loadUser(OAuth2UserRequest userRequest)
            throws OAuth2AuthenticationException
    {
        OAuth2User oauth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo oAuth2UserInfo = switch (registrationId) {
            case "google" -> new GoogleUserInfo(oauth2User.getAttributes());
            case "naver" -> new NaverUserInfo(oauth2User.getAttribute("response"));
            case "kakao" -> new KakaoUserInfo(oauth2User.getAttributes(), oauth2User.getAttribute("kakao_account"));
            case "github" -> new GithubUserInfo(oauth2User.getAttributes());
            default -> throw new CommonException(AuthErrorCode.UNSUPPORTED_OAUTH_PROVIDER);
        };

        Optional<User> user = oAuthService.findUser(oAuth2UserInfo.getProvider(), oAuth2UserInfo.getProviderId());
        User oAuthUser = user.orElseGet( () ->
                userService.registerSocialUser(oAuth2UserInfo.getEmail(), oAuth2UserInfo.getName(), oAuth2UserInfo.getProfileUrl())
        );

        // 소셜 가입 시 기본 아바타·초기 포인트는 가입 트랜잭션에서 함께 생성 (UserRegisteredEvent)
        if (user.isEmpty()) oAuthService.link(oAuthUser, oAuth2UserInfo.getProvider(), oAuth2UserInfo.getProviderId());
        // 탈퇴 유예 계정 복구, 정지·휴면 계정은 소셜 로그인도 거부 (OAuth2 실패 핸들러로 전달)
        try {
            userService.restoreIfWithdrawn(oAuthUser);
        } catch (CommonException e) {
            throw new OAuth2AuthenticationException(new OAuth2Error("account_withdrawn"), e.getMessage());
        }
        try {
            suspensionService.validateNotSuspended(oAuthUser);
        } catch (CommonException e) {
            throw new OAuth2AuthenticationException(new OAuth2Error("account_suspended"), e.getMessage());
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
