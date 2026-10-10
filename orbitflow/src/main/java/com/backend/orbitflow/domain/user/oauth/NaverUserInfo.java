package com.backend.orbitflow.domain.user.oauth;

import com.backend.orbitflow.domain.user.enums.Provider;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import java.util.Map;

public record NaverUserInfo(
        Map<String, Object> attributes
) implements OAuth2UserInfo {

    @Override
    public String getProviderId() {
        return (String) attributes.get("id");
    }

    @Override
    public Provider getProvider() {
        return Provider.NAVER;
    }

    // 이메일 제공에 동의하지 않으면 없음 → 가입 불가이므로 OAuth2 실패 핸들러로 전달 (Kakao·GitHub 와 동일)
    @Override
    public String getEmail() {
        Object email = attributes.get("email");
        if (email == null || email.toString().isBlank()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("email_not_provided"), "소셜 계정에서 이메일을 가져올 수 없습니다.");
        }
        return email.toString();
    }

    @Override
    public String getName() {
        return (String) attributes.get("nickname");
    }

    @Override
    public String getProfileUrl() {
        return (String) attributes.get("profile_image");
    }
}
