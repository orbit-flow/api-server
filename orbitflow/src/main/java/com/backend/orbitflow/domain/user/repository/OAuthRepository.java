package com.backend.orbitflow.domain.user.repository;

import com.backend.orbitflow.domain.user.entity.OAuthAccount;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.domain.user.enums.Provider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OAuthRepository extends JpaRepository<OAuthAccount, Long> {

    Optional<OAuthAccount> findByProviderAndProviderId(Provider provider, String providerId);

    List<OAuthAccount> findAllByUserOrderByCreatedAtAsc(User user);

    Optional<OAuthAccount> findByUserAndProvider(User user, Provider provider);

    long countByUser(User user);
}
