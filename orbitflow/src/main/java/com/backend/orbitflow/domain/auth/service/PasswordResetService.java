package com.backend.orbitflow.domain.auth.service;

public interface PasswordResetService {

    void checkCooldown(String email);
    String issue(String userUuid);
    String consume(String token);
}
