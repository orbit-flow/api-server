package com.backend.orbitflow.domain.user.service;

import com.backend.orbitflow.domain.user.entity.OAuthAccount;
import com.backend.orbitflow.domain.user.enums.Provider;
import com.backend.orbitflow.domain.user.entity.User;

import java.util.Optional;
import com.backend.orbitflow.domain.user.entity.OAuthAccount;
import org.springframework.data.domain.Page;

public interface OAuthService {
    Optional<User> findUser(Provider provider, String providerId);
    void link(User user, Provider provider, String providerId);
    Page<OAuthAccount> getLinkedAccounts(User user, int page, int size);
    void unlink(User user, Provider provider);
}
