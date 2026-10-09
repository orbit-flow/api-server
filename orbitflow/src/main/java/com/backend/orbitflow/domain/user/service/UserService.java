package com.backend.orbitflow.domain.user.service;

import com.backend.orbitflow.domain.user.entity.User;

public interface UserService {

    User getByUuid(String uuid);
    User getByEmail(String email);
    User register(String email, String password, String name, String varifyToken);
    User registerSocialUser(String email, String name, String profileImage);
    User updateProfile(String uuid, String name, String profileImage, String introduce, boolean isPrivate);
    User updateEmail(String uuid, String email);
    User updatePassword(String uuid, String password);
    void deleteUser(String uuid, String confirmName);
    void updateLastLoginAt(User user);
}
