package com.backend.orbitflow.domain.suspension.entity;

import com.backend.orbitflow.domain.suspension.enums.SuspensionStatus;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "suspensions")
public class Suspension extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SuspensionStatus state;

    @Column(nullable = false)
    private String reason;

    // null이면 영구 정지
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    // 정지 처리한 관리자
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "suspended_by", nullable = false)
    private User suspendedBy;

    @Column(name = "released_reason")
    private String releasedReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "released_by")
    private User releasedBy;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;
}
