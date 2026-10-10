package com.backend.orbitflow.domain.user.entity;

import com.backend.orbitflow.domain.user.enums.Provider;
import com.backend.orbitflow.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// 소셜 계정은 사용자당 1개만 연결 : user_id 유일, 같은 제공자 계정의 중복 가입도 (provider, provider_id) 로 차단
@Table(name = "social_users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"provider", "provider_id"}),
                @UniqueConstraint(columnNames = {"user_id"})
        })
public class OAuthAccount extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Provider provider;

    @Column(name = "provider_id", nullable = false)
    private String providerId;

    public static OAuthAccount of(
            User user, Provider provider, String providerId
    ) {
        return new OAuthAccount(
                null, user, provider, providerId
        );
    }
}
