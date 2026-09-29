package com.backend.orbitflow.domain.user.entity;

import com.backend.orbitflow.domain.user.enums.UserRole;
import com.backend.orbitflow.domain.user.enums.UserState;
import com.backend.orbitflow.global.common.entity.SoftDeleteEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
public class User extends SoftDeleteEntity{

    @Id  @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String uuid;

    @Column(unique = true)
    private String email;

    private String password;

    private String name;
    private String introduce;
    private String profileImage;
    
    @Enumerated(EnumType.STRING)
    private UserRole role;
    @Enumerated(EnumType.STRING)
    private UserState state;
    
    private boolean isPrivate;

    public static User of(
        String uuid, String email, String password, String name, UserRole role
    ) {
        return new User(
            null, uuid, email, password, name, null, null, role, UserState.ACTIVE, false
        );
    }

    public void updateUserInfo(String name, String introduce, String profileImage) {
        this.name = name;
        this.introduce = introduce;
        this.profileImage = profileImage;
    }

    public void updateEmail(String email) {
        this.email = email;
    }

    public void updateUserState(UserState state) {
        this.state = state;
    }
}
