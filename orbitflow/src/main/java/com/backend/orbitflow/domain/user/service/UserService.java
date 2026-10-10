package com.backend.orbitflow.domain.user.service;

import java.util.Optional;

import com.backend.orbitflow.domain.user.entity.User;
import java.util.Collection;
import java.util.List;

public interface UserService {

    User getByUuid(String uuid);
    List<User> getAllByUuids(Collection<String> uuids);
    User getByUuidIncludingBanned(String uuid);
    User getByEmail(String email);
    Optional<User> findByEmail(String email);
    Optional<User> findById(Long id);
    User resetPassword(String uuid, String encodedPassword);
    User register(String email, String password, String name, String varifyToken);
    User registerSocialUser(String email, String name, String profileImage);
    User updateProfile(String uuid, String name, String introduce, boolean isPrivate);
    String updateProfileImage(String uuid, String profileImage);
    User updateEmail(String uuid, String email, String varifyToken);
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
