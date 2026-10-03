package com.backend.orbitflow.domain.user.repository;

import com.backend.orbitflow.domain.user.entity.OAuthAccount;
import com.backend.orbitflow.domain.user.enums.Provider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OAuthRepository extends JpaRepository<OAuthAccount, Long> {

    Optional<OAuthAccount> findByProviderAndProviderId(Provider provider, String providerId);
}
