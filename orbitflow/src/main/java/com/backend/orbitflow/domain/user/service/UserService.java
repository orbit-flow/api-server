package com.backend.orbitflow.domain.user.service;

import java.util.Optional;

import com.backend.orbitflow.domain.user.entity.User;
import java.util.Collection;
import java.util.List;

public interface UserService {

    // 로그인한 본인 조회 : ACTIVE 전용 (정지·휴면·탈퇴 제외)
    User getActiveByUuid(String uuid);
    // 다른 사용자 조회 : 휴면 계정도 대상 (정지·탈퇴 제외)
    User getByUuid(String uuid);
    List<User> getAllByUuids(Collection<String> uuids);
    User getByUuidIncludingBanned(String uuid);
    Optional<User> findByEmail(String email);
    Optional<User> findById(Long id);
    User resetPassword(String uuid, String encodedPassword);
    User register(String email, String password, String name);
    Optional<User> releaseWithdrawnEmail(String email);
    User registerSocialUser(String email, String name, String profileImage);
    User getViewableUser(User target, boolean blocked);
    User updateProfile(String uuid, String name, String introduce, boolean isPrivate);
    User replaceProfileImage(String uuid, String profileImage);
    User updateEmail(String uuid, String email);
    User updatePassword(String uuid, String password);
    User lockUser(Long userId);
    User setInitialPassword(String uuid, String encodedPassword);
    User updateChatInviteSetting(String uuid, boolean allowNonFollowChatInvite);
    void deleteUser(String uuid);
    void restoreIfWithdrawn(User user);
    void validateNotDormant(User user);
    void releaseDormant(User user);
    int convertDormantUsers();
    void updateLastLoginAt(User user);
}
