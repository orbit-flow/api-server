package com.backend.orbitflow.domain.user.oauth;

import com.backend.orbitflow.domain.user.enums.Provider;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import java.util.Map;

public record GithubUserInfo(
        Map<String, Object> attributes
) implements OAuth2UserInfo {

    private static final int NAME_MAX_LENGTH = 50; // users.name 컬럼 길이

    // GitHub id는 숫자
    @Override
    public String getProviderId() {
        return String.valueOf(attributes.get("id"));
    }

    @Override
    public Provider getProvider() {
        return Provider.GITHUB;
    }

    // 이메일 비공개 계정은 email이 null → 가입 불가이므로 OAuth2 실패 핸들러로 전달
    @Override
    public String getEmail() {
        Object email = attributes.get("email");
        if (email == null || email.toString().isBlank()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("email_not_provided"), "소셜 계정에서 이메일을 가져올 수 없습니다.");
        }
        return email.toString();
    }

    // name이 없으면 login(아이디), 그것도 없으면 이메일 앞부분 사용
    @Override
    public String getName() {
        Object name = attributes.get("name");
        if (name == null || name.toString().isBlank()) {
            name = attributes.get("login");
        }
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
        return (String) attributes.get("avatar_url");
    }
}
