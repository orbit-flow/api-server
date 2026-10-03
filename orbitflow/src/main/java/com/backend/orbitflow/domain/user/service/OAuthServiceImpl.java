package com.backend.orbitflow.domain.user.service;

import com.backend.orbitflow.domain.user.entity.OAuthAccount;
import com.backend.orbitflow.domain.user.enums.Provider;
import com.backend.orbitflow.domain.user.repository.OAuthRepository;
import com.backend.orbitflow.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class OAuthServiceImpl implements OAuthService{

    private final OAuthRepository oAuthRepository;

    public Optional<User> findUser(Provider provider, String providerId) {
        return oAuthRepository.findByProviderAndProviderId(provider, providerId)
                .map(OAuthAccount::getUser);
    }

    public void link(User user, Provider provider, String providerId) {
        OAuthAccount oAuthAccount = OAuthAccount.of(user, provider, providerId);
        oAuthRepository.save(oAuthAccount);
    }

}
