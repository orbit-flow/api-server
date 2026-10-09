package com.backend.orbitflow.domain.user.service;

import java.util.List;
import com.backend.orbitflow.global.common.error.exception.CommonException;
import com.backend.orbitflow.domain.user.error.UserErrorCode;
import com.backend.orbitflow.domain.user.entity.OAuthAccount;
import com.backend.orbitflow.domain.user.enums.Provider;
import com.backend.orbitflow.domain.user.repository.OAuthRepository;
import com.backend.orbitflow.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class OAuthServiceImpl implements OAuthService{

    private final OAuthRepository oAuthRepository;
    private final UserService userService;

    public Optional<User> findUser(Provider provider, String providerId) {
        return oAuthRepository.findByProviderAndProviderId(provider, providerId)
                .map(OAuthAccount::getUser);
    }

    public void link(User user, Provider provider, String providerId) {
        OAuthAccount oAuthAccount = OAuthAccount.of(user, provider, providerId);
        oAuthRepository.save(oAuthAccount);
    }

    @Transactional(readOnly = true)
    public List<OAuthAccount> getLinkedAccounts(User user) {
        return oAuthRepository.findAllByUserOrderByCreatedAtAsc(user);
    }

    // 비밀번호가 있거나 다른 소셜 계정이 남아 있을 때만 해제 (로그인 수단이 없어지는 것 방지)
    // 사용자 행 락으로 같은 사용자의 동시 해제·비밀번호 설정 요청을 직렬화
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void unlink(User user, Provider provider) {
        User locked = userService.lockUser(user.getId());
        OAuthAccount account = oAuthRepository.findByUserAndProvider(locked, provider).orElseThrow(
                () -> new CommonException(UserErrorCode.OAUTH_ACCOUNT_NOT_FOUND)
        );
        if (locked.getPassword() == null && oAuthRepository.countByUser(locked) <= 1) {
            throw new CommonException(UserErrorCode.LAST_LOGIN_METHOD);
        }
        oAuthRepository.delete(account);
    }

}
