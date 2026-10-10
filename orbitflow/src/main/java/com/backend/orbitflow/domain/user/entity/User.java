package com.backend.orbitflow.domain.user.entity;

import com.backend.orbitflow.domain.user.enums.UserRole;
import com.backend.orbitflow.domain.user.enums.UserStatus;
import com.backend.orbitflow.global.common.entity.SoftDeleteEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;
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

    @Column(nullable = false, unique = true, length = 36)
    private String uuid;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    // 소셜 가입 사용자는 null
    private String password;

    @Column(nullable = false, length = 50)
    private String name;
    private String introduce;
    private String profileImage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @Column(nullable = false)
    private boolean isPrivate;

    private LocalDateTime lastLoginAt;

    @Column(nullable = false)
    private boolean allowNonFollowChatInvite;

    // 이용약관·개인정보 처리방침 동의 일시 (가입 시점에 기록, 이후 변경하지 않음)
    // 기존 행이 있는 DB에 컬럼이 추가될 때 NOT NULL 위반이 나지 않도록 DB 기본값을 둠 (기존 사용자는 컬럼 추가 시각)
    @ColumnDefault("CURRENT_TIMESTAMP(6)")
    @Column(nullable = false, updatable = false)
    private LocalDateTime termsAgreedAt;

    public static User of(
        String uuid, String email, String password, String name, UserRole role
    ) {
        return new User(
            null, uuid, email, password, name, null, null, role, UserStatus.ACTIVE, false, null, true, LocalDateTime.now()
        );
    }

    public static User social(
            String uuid, String email, String name, String profileImage
    ) {
        return new User (
                null, uuid, email, null, name, null, profileImage, UserRole.ROLE_USER, UserStatus.ACTIVE, false, null, true, LocalDateTime.now()
        );
    }

    public void updateLastLoginAt() {
        this.lastLoginAt = LocalDateTime.now();
    }

    public void updateUserInfo(String name, String introduce, boolean isPrivate) {
        this.name = name;
        this.introduce = introduce;
        this.isPrivate = isPrivate;
    }

    // null이면 FE 기본 이미지
    public void updateProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public void updateEmail(String email) {
        this.email = email;
    }

    public void updateUserStatus(UserStatus status) {
        this.status = status;
    }

    // 팔로우 중이 아닌 사용자의 대화 초대 허용 여부
    public void updateAllowNonFollowChatInvite(boolean allow) {
        this.allowNonFollowChatInvite = allow;
    }

    // 탈퇴 후 30일 경과 시 식별정보 파기 (결제·포인트 거래 등 보관 기록의 참조를 위해 행은 유지)
    public void anonymize(String anonymizedEmail) {
        this.email = anonymizedEmail;
        this.name = "탈퇴한 사용자";
        this.password = null;
        this.introduce = null;
        this.profileImage = null;
    }

    public void updatePassword(String password) {
        this.password = password;
    }
}
