package com.backend.orbitflow.domain.user.repository;

import com.backend.orbitflow.domain.user.entity.OAuthAccount;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.domain.user.enums.Provider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OAuthRepository extends JpaRepository<OAuthAccount, Long> {

    Optional<OAuthAccount> findByProviderAndProviderId(Provider provider, String providerId);

    Page<OAuthAccount> findAllByUserOrderByCreatedAtAsc(User user, Pageable pageable);

    Optional<OAuthAccount> findByUserAndProvider(User user, Provider provider);

    long countByUser(User user);
}
