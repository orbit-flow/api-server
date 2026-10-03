package com.backend.orbitflow.domain.user.oauth;

import com.backend.orbitflow.domain.user.enums.Provider;

import java.util.Map;

public record GithubUserInfo(
        Map<String, Object> attributes
) implements OAuth2UserInfo {

    @Override
    public String getProviderId() {
        return (String) attributes.get("id");
    }

    @Override
    public Provider getProvider() {
        return Provider.GITHUB;
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getName() {
        return (String) attributes.get("name");
    }

    @Override
    public String getProfileUrl() {
        return (String) attributes.get("avatar_url");
    }
}
