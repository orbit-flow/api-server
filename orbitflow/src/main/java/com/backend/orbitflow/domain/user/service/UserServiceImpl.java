package com.backend.orbitflow.domain.user.service;

import com.backend.orbitflow.domain.auth.error.AuthErrorCode;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.domain.user.enums.UserRole;
import com.backend.orbitflow.domain.user.enums.UserStatus;
import com.backend.orbitflow.domain.user.error.UserErrorCode;
import com.backend.orbitflow.global.common.error.exception.CommonException;
import com.backend.orbitflow.global.util.S3Service;
import com.backend.orbitflow.global.util.S3TransactionalFileManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.backend.orbitflow.domain.user.repository.UserRepository;

import com.backend.orbitflow.domain.user.event.UserWithdrawnEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService{

    private static final int WITHDRAWAL_RETENTION_DAYS = 30;
    private static final int DORMANT_MONTHS = 12;
    private static final String RELEASED_EMAIL_PREFIX = "released-";

    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final EntityManager entityManager;
    private final S3Service s3Service;
    private final S3TransactionalFileManager s3FileManager;

    // 로그인한 본인의 변경·조회 : 정지·휴면·탈퇴 계정은 대상이 아님 (휴면 계정은 로그인·이메일 인증 절차로만 활성화)
    @Transactional(readOnly = true)
    public User getActiveByUuid(String uuid) {
        return userRepository.findByUuidAndDeletedAtIsNullAndStatus(uuid, UserStatus.ACTIVE).orElseThrow(
                () -> new CommonException(UserErrorCode.USER_NOT_FOUND)
        );
    }

    // 다른 사용자를 찾는 경우(프로필 열람, 팔로우·초대 대상 등) : 휴면 계정도 존재하는 사용자이므로 ACTIVE 로 제한하지 않음
    @Transactional(readOnly = true)
    public User getByUuid(String uuid) {
        return userRepository.findByUuidAndDeletedAtIsNullAndStatusNot(uuid, UserStatus.BANNED).orElseThrow(
                () -> new CommonException(UserErrorCode.USER_NOT_FOUND)
        );
    }

    // 차단 관계(양방향)인 사용자는 존재하지 않는 것으로 처리 : 차단이 공개 범위·팔로우 상태보다 우선
    public User getViewableUser(User target, boolean blocked) {
        if (blocked) {
            throw new CommonException(UserErrorCode.USER_NOT_FOUND);
        }
        return target;
    }

    // uuid 목록을 한 번에 조회 (요청 순서 유지, 중복 제거), 하나라도 없으면 USER_NOT_FOUND
    @Transactional(readOnly = true)
    public List<User> getAllByUuids(Collection<String> uuids) {
        List<String> distinct = uuids.stream().distinct().toList();
        if (distinct.isEmpty()) {
            return List.of();
        }
        Map<String, User> byUuid = userRepository.findAllByUuidInAndDeletedAtIsNullAndStatusNot(distinct, UserStatus.BANNED).stream()
                .collect(Collectors.toMap(User::getUuid, user -> user));
        if (byUuid.size() != distinct.size()) {
            throw new CommonException(UserErrorCode.USER_NOT_FOUND);
        }
        return distinct.stream().map(byUuid::get).toList();
    }

    // 정지 계정 포함 조회 (관리자 정지 처리용)
    @Transactional(readOnly = true)
    public User getByUuidIncludingBanned(String uuid) {
        return userRepository.findByUuidAndDeletedAtIsNull(uuid).orElseThrow(
                () -> new CommonException(UserErrorCode.USER_NOT_FOUND)
        );
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // 탈퇴·정지 여부와 무관하게 조회 (상태 판단은 호출 측)
    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    // 비밀번호 분실 재설정 : 이메일 소유가 확인되었으므로 휴면 상태도 함께 해제
    // 탈퇴 유예 중인 계정은 재설정 후 로그인하면 복구되고, 정지 계정은 재설정해도 로그인이 제한됨
    public User resetPassword(String uuid, String encodedPassword) {
        User user = userRepository.findByUuid(uuid).orElseThrow(
                () -> new CommonException(UserErrorCode.USER_NOT_FOUND)
        );
        user.updatePassword(encodedPassword);
        if (user.getStatus() == UserStatus.SLEEP) {
            user.updateUserStatus(UserStatus.ACTIVE);
            user.updateLastLoginAt();
        }
        return user;
    }

    // 이메일 인증 토큰 검증은 호출 측(UserFacade)이 먼저 수행
    public User register(String email, String password, String name) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new CommonException(UserErrorCode.EMAIL_DUPLICATE);
        }
        User user = User.of(
                UUID.randomUUID().toString().replace("-", ""),
                email,
                password,
                name,
                UserRole.ROLE_USER
        );
        return userRepository.save(user);
    }

    // 탈퇴한 계정(30일 유예 중)이 점유한 이메일을 풀어 재가입을 허용 : 이전 계정은 식별정보를 지워 복구할 수 없게 함
    // 이메일은 purge 가 "이미 처리됨"으로 보는 withdrawn- 접두어를 쓰지 않아, 보관 기간이 지나면 purge 가 남은 데이터를 정리함
    // 새 행의 INSERT(IDENTITY)가 먼저 실행되어 이메일 unique 에 걸리지 않도록 즉시 flush
    // 소셜 연결 삭제는 호출 측(Facade)이 반환된 이전 계정으로 수행
    // 동시 로그인(복구)·동시 가입과 직렬화하려고 행 락을 잡고 최신 상태로 다시 확인 (락 이후 확인은 호출 측의 READ_COMMITTED 필요)
    // 이전 프로필 이미지는 anonymize 로 참조가 사라지므로 여기서 커밋 후 삭제 (이후 purge 는 null 이라 지우지 못함)
    public Optional<User> releaseWithdrawnEmail(String email) {
        return userRepository.findByEmail(email)
                .filter(found -> found.getDeletedAt() != null)
                .flatMap(found -> {
                    User locked = lockUser(found.getId());
                    if (locked.getDeletedAt() == null || !email.equals(locked.getEmail())) {
                        return Optional.empty();
                    }
                    String oldImage = locked.getProfileImage();
                    locked.anonymize(RELEASED_EMAIL_PREFIX + locked.getUuid() + "@orbitflow.invalid");
                    userRepository.saveAndFlush(locked);
                    deleteManagedImage(oldImage);
                    return Optional.of(locked);
                });
    }

    public User registerSocialUser(String email, String name, String profileImage) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new CommonException(UserErrorCode.EMAIL_DUPLICATE);
        }
        User user = User.social(
                UUID.randomUUID().toString().replace("-", ""),
                email,
                name,
                profileImage
        );
        return userRepository.save(user);
    }

    public User updateProfile(String uuid, String name, String introduce, boolean isPrivate) {
        User user = getActiveByUuid(uuid);
        user.updateUserInfo(name, introduce, isPrivate);
        return userRepository.save(user);
    }

    // DB 작업은 사용자 조회 SELECT 1회 + 변경 UPDATE 1회 (더티 체킹), 변경 전 이미지는 커밋 후 삭제
    public User replaceProfileImage(String uuid, String profileImage) {
        User user = getActiveByUuid(uuid);
        String oldImage = user.getProfileImage();
        user.updateProfileImage(profileImage);
        deleteManagedImage(oldImage);
        return user;
    }

    // 새 이메일의 OTP 인증 토큰 검증은 호출 측(UserFacade)이 먼저 수행 (가입과 동일)
    public User updateEmail(String uuid, String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new CommonException(UserErrorCode.EMAIL_DUPLICATE);
        }
        User user = getActiveByUuid(uuid);
        user.updateEmail(email);
        return userRepository.save(user);
    }

    public User updateChatInviteSetting(String uuid, boolean allowNonFollowChatInvite) {
        User user = getActiveByUuid(uuid);
        user.updateAllowNonFollowChatInvite(allowNonFollowChatInvite);
        return user;
    }

    // 로그인 수단 변경(비밀번호 설정·소셜 연결 해제)을 사용자 단위로 직렬화 : 동시 해제로 로그인 수단이 모두 사라지는 것 방지
    // 이미 영속성 컨텍스트에 있던 오래된 상태를 쓰지 않도록 락과 함께 최신 상태로 갱신
    public User lockUser(Long userId) {
        User user = entityManager.find(User.class, userId);
        if (user == null) {
            throw new CommonException(UserErrorCode.USER_NOT_FOUND);
        }
        entityManager.refresh(user, LockModeType.PESSIMISTIC_WRITE);
        return user;
    }

    // 소셜 가입자(비밀번호 없음)만 최초 설정 가능, 사용자 행 락으로 소셜 연결 해제와 직렬화
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public User setInitialPassword(String uuid, String encodedPassword) {
        User user = lockUser(getActiveByUuid(uuid).getId());
        if (user.getPassword() != null) {
            throw new CommonException(UserErrorCode.PASSWORD_ALREADY_SET);
        }
        user.updatePassword(encodedPassword);
        return user;
    }

    public User updatePassword(String uuid, String password) {
        User user = getActiveByUuid(uuid);
        user.updatePassword(password);
        return userRepository.save(user);
    }

    // 탈퇴 신청 시점부터 30일간 논리적 삭제 상태로 보관 (본인 확인은 호출 측에서 수행)
    public void deleteUser(String uuid) {
        User user = getActiveByUuid(uuid);
        user.delete();
        userRepository.save(user);
        eventPublisher.publishEvent(new UserWithdrawnEvent(uuid));
    }

    // 탈퇴 유예 기간(30일) 내 로그인 시 계정 복구, 기간이 지났으면 존재하지 않는 계정으로 처리
    public void restoreIfWithdrawn(User user) {
        if (user.getDeletedAt() == null) {
            return;
        }
        if (user.getDeletedAt().isBefore(LocalDateTime.now().minusDays(WITHDRAWAL_RETENTION_DAYS))) {
            throw new CommonException(AuthErrorCode.LOGIN_FAIL);
        }
        user.restore();
    }

    public void validateNotDormant(User user) {
        if (user.getStatus() == UserStatus.SLEEP) {
            throw new CommonException(AuthErrorCode.ACCOUNT_DORMANT);
        }
    }

    // 일회성 이메일 인증을 마친 휴면 계정 재활성화 (재전환 방지를 위해 마지막 로그인 시각 갱신)
    public void releaseDormant(User user) {
        if (user.getStatus() != UserStatus.SLEEP) {
            throw new CommonException(AuthErrorCode.NOT_DORMANT_ACCOUNT);
        }
        user.updateUserStatus(UserStatus.ACTIVE);
        user.updateLastLoginAt();
        userRepository.save(user);
    }

    // 마지막 로그인으로부터 12개월이 지난 계정 휴면 전환 (스케줄러)
    public int convertDormantUsers() {
        return userRepository.convertToDormant(LocalDateTime.now().minusMonths(DORMANT_MONTHS));
    }

    // 로그인 시점에는 탈퇴 유예 계정도 대상이 되므로 uuid 재조회 없이 엔티티를 받음
    public void updateLastLoginAt(User user) {
        user.updateLastLoginAt();
        userRepository.save(user);
    }

    // 소셜 프로필 등 외부 URL은 삭제하지 않고, 우리 S3 파일만 커밋 후 삭제
    private void deleteManagedImage(String imageUrl) {
        if (s3Service.isManagedFile(imageUrl)) {
            s3FileManager.deleteAfterCommit(List.of(imageUrl));
        }
    }

}
