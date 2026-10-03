package com.backend.orbitflow.domain.user.service;

import com.backend.orbitflow.domain.user.entity.OAuthAccount;
import com.backend.orbitflow.domain.user.enums.Provider;
import com.backend.orbitflow.domain.user.entity.User;

import java.util.Optional;

public interface OAuthService {
    Optional<User> findUser(Provider provider, String providerId);
    void link(User user, Provider provider, String providerId);
}
