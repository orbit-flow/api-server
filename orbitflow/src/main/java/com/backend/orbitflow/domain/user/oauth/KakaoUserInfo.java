package com.backend.orbitflow.domain.user.oauth;

import com.backend.orbitflow.domain.user.enums.Provider;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import java.util.Map;

public record KakaoUserInfo(
        Map<String, Object> attributes,
        Map<String, Object> kakaoAccount
) implements OAuth2UserInfo{

    private static final int NAME_MAX_LENGTH = 50; // users.name 컬럼 길이

    // Kakao id는 숫자
    @Override
    public String getProviderId() {
        return String.valueOf(attributes.get("id"));
    }

    @Override
    public Provider getProvider() {
        return Provider.KAKAO;
    }

    // 이메일은 kakao_account 안에 있으며, 동의하지 않으면 없음 → 가입 불가이므로 OAuth2 실패 핸들러로 전달
    @Override
    public String getEmail() {
        Object email = kakaoAccount == null ? null : kakaoAccount.get("email");
        if (email == null || email.toString().isBlank()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("email_not_provided"), "소셜 계정에서 이메일을 가져올 수 없습니다.");
        }
        return email.toString();
    }

    // 닉네임이 없으면 이메일 앞부분 사용
    @Override
    public String getName() {
        Map<String, Object> profile = getProfile();
        Object name = profile == null ? null : profile.get("nickname");
        if (name == null || name.toString().isBlank()) {
            name = getEmail().split("@")[0];
        }
        String result = name.toString();
        return result.codePointCount(0, result.length()) > NAME_MAX_LENGTH
                ? result.substring(0, result.offsetByCodePoints(0, NAME_MAX_LENGTH))
                : result;
    }

    @Override
    public String getProfileUrl() {
        Map<String, Object> profile = getProfile();
        return profile == null ? "" : (String) profile.get("profile_image_url");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getProfile() {
        return kakaoAccount == null ? null : (Map<String, Object>) kakaoAccount.get("profile");
    }
}
