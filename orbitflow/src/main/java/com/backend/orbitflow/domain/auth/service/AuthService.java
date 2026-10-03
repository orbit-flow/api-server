package com.backend.orbitflow.domain.auth.service;

import com.backend.orbitflow.domain.user.entity.User;

public interface AuthService {

    String encodePassword(String rawPassword);
    void verifyPassword(String rawPassword, String encodedPassword);
    void authenticate(User user, String password);
    String createCode(String email, Long time);
    String varifyEmail(String email, String code, Long expireTime);
}
