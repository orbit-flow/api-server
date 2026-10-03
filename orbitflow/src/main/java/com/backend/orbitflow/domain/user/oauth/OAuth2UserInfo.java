package com.backend.orbitflow.domain.user.oauth;

import com.backend.orbitflow.domain.user.enums.Provider;

public interface OAuth2UserInfo {
    String getProviderId();
    Provider getProvider();
    String getEmail();
    String getName();
    String getProfileUrl();
}
