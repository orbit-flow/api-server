package com.backend.orbitflow.domain.user.service;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

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

    @Transactional(readOnly = true)
    public Page<OAuthAccount> getLinkedAccounts(User user, int page, int size) {
        Pageable pageable = toPageable(page, size);
        return oAuthRepository.findAllByUserOrderByCreatedAtAsc(user, pageable);
    }

    // 소셜 계정은 사용자당 1개(user_id 유일)이므로 비밀번호가 있을 때만 해제 (로그인 수단이 없어지는 것 방지)
    // 사용자 행 락은 호출 측(UserFacade)이 먼저 획득한 사용자를 넘겨 같은 사용자의 동시 해제·비밀번호 설정 요청을 직렬화
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void unlink(User lockedUser, Provider provider) {
        OAuthAccount account = oAuthRepository.findByUserAndProvider(lockedUser, provider).orElseThrow(
                () -> new CommonException(UserErrorCode.OAUTH_ACCOUNT_NOT_FOUND)
        );
        if (lockedUser.getPassword() == null) {
            throw new CommonException(UserErrorCode.LAST_LOGIN_METHOD);
        }
        oAuthRepository.delete(account);
    }

    // 탈퇴 계정의 이메일을 풀어 줄 때 함께 호출 : 소셜 연결을 모두 삭제해 같은 소셜 계정으로 새로 가입할 수 있게 함
    public void unlinkAll(User user) {
        oAuthRepository.deleteByUser(user);
    }

    // 요청 page는 1부터 시작
    private Pageable toPageable(int page, int size) {
        return PageRequest.of(Math.max(page - 1, 0), Math.min(Math.max(size, 1), 100));
    }
}
