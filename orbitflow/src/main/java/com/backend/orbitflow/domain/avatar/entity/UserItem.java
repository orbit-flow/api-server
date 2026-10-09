package com.backend.orbitflow.domain.avatar.entity;

import com.backend.orbitflow.domain.item.entity.Item;
import com.backend.orbitflow.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 보유 아이템 : 같은 아이템은 아바타당 1개만 보유
@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_items",
        uniqueConstraints = @UniqueConstraint(columnNames = {"avatar_id", "item_id"}))
public class UserItem extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "avatar_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Avatar avatar;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(nullable = false)
    private boolean isEquipped;

    public static UserItem of(Avatar avatar, Item item) {
        return new UserItem(
                null, avatar, item, false
        );
    }

    public void equip() {
        this.isEquipped = true;
    }

    public void unequip() {
        this.isEquipped = false;
    }
}
