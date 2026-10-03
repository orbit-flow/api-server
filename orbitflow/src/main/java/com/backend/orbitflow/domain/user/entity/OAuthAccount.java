package com.backend.orbitflow.domain.user.entity;

import com.backend.orbitflow.domain.user.enums.Provider;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "oauth_accounts",
        uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "provider_id"}))
public class OAuthAccount {

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
