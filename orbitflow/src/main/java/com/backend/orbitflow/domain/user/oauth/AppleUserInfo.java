package com.backend.orbitflow.domain.user.oauth;

import com.backend.orbitflow.domain.user.enums.Provider;

import java.util.Map;

public record AppleUserInfo(
        Map<String, Object> attributes,
        Map<String, Object> nameMap
) implements OAuth2UserInfo{

    @Override
    public String getProviderId() {
        return (String) attributes.get("sub");
    }

    @Override
    public Provider getProvider() {
        return Provider.APPLE;
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getName() {
        if(nameMap == null) {
            return "Apple User";
        }
        String firstName = (String) nameMap.get("firstName");
        String lastName = (String) nameMap.get("lastName");

        return ((firstName != null) ? firstName : "")
                + ((lastName != null) ? lastName : "");
    }

    @Override
    public String getProfileUrl() {
        return null;
    }
}
