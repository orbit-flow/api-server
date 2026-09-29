package com.backend.orbitflow.global.security;

import com.backend.orbitflow.domain.user.enums.UserRole;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Collections;

@Getter 
public class AuthUser {

    private final String uuid;
    private final String email;
    private final UserRole role;

    public AuthUser(String uuid, String email, UserRole role) {
        this.uuid = uuid;
        this.email = email;
        this.role = role;
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority(role.name));
    }
}
