package com.backend.orbitflow.domain.auth.advice;

import com.backend.orbitflow.domain.auth.error.AuthErrorCode;
import com.backend.orbitflow.domain.auth.facade.OAuthFacade;
import com.backend.orbitflow.domain.user.oauth.GithubUserInfo;
import com.backend.orbitflow.domain.user.oauth.GoogleUserInfo;
import com.backend.orbitflow.domain.user.oauth.KakaoUserInfo;
import com.backend.orbitflow.domain.user.oauth.NaverUserInfo;
import com.backend.orbitflow.domain.user.oauth.OAuth2UserInfo;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

// 소셜 로그인 사용자 정보 조회 : 제공자 호출(외부 HTTP)은 DB 트랜잭션 밖에서 수행하고, 가입·로그인 처리는 OAuthFacade 의 트랜잭션에 위임
@Component
@RequiredArgsConstructor
public class OAuth2UserLoader extends DefaultOAuth2UserService {

    private final OAuthFacade oAuthFacade;

    @Override
    @NullMarked
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo oAuth2UserInfo = switch (registrationId) {
            case "google" -> new GoogleUserInfo(oauth2User.getAttributes());
            case "naver" -> new NaverUserInfo(oauth2User.getAttribute("response"));
            case "kakao" -> new KakaoUserInfo(oauth2User.getAttributes(), oauth2User.getAttribute("kakao_account"));
            case "github" -> new GithubUserInfo(oauth2User.getAttributes());
            // OAuth2 필터는 AuthenticationException만 실패 핸들러로 넘기므로 FE 로그인 화면으로 돌려보내도록 변환
            default -> throw new OAuth2AuthenticationException(new OAuth2Error("unsupported_provider"),
                    AuthErrorCode.UNSUPPORTED_OAUTH_PROVIDER.getMessage());
        };
        return oAuthFacade.loadOrRegister(oAuth2UserInfo);
    }
}
