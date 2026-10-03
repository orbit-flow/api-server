package com.backend.orbitflow.domain.user.oauth;

import com.backend.orbitflow.domain.user.enums.Provider;

import java.util.Map;

public record KakaoUserInfo(
        Map<String, Object> attributes,
        Map<String, Object> kakaoAccount
) implements OAuth2UserInfo{

    @Override
    public String getProviderId() {
        return (String) attributes.get("id");
    }

    @Override
    public Provider getProvider() {
        return Provider.KAKAO;
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getName() {
        Map<String, Object> profile = getProfile();
        return profile == null ? "" : (String) profile.get("nickname");
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
