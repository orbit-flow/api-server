package com.backend.orbitflow.domain.auth.service;

public interface AuthService {

    String encodePassword(String rawPassword);
    void verifyPassword(String rawPassword, String encodedPassword);
}
