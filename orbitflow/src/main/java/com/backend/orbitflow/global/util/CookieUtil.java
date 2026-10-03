package com.backend.orbitflow.global.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CookieUtil {
    public static ResponseCookie createCookie(String name, String value, long duration) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(Duration.ofMillis(duration))
                .sameSite("Strict")
                .build();
    }
}
