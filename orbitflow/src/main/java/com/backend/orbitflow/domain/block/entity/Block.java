package com.backend.orbitflow.domain.block.entity;

import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "blocks",
        uniqueConstraints = @UniqueConstraint(columnNames = {"blocker_id", "blockee_id"})
)
public class Block extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blocker_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User blocker;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blockee_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User blockee;

    public static Block of(User blocker, User blockee) {
        return new Block(
                null, blocker, blockee
        );
    }
}
