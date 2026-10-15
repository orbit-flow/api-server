package com.backend.orbitflow.domain.auth.service;

import com.backend.orbitflow.domain.user.entity.User;

public interface AuthService {

    String encodePassword(String rawPassword);
    void verifyPassword(String rawPassword, String encodedPassword);
    void authenticate(User user, String password);
    String createCode(String email);
    long getCodeExpireMillis();
    String verifyEmail(String email, String code);
    void verifyCode(String email, String code);
    void consumeVerifyToken(String email, String token);
}
