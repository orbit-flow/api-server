package com.backend.orbitflow.domain.item.entity;

import com.backend.orbitflow.domain.item.enums.ItemType;
import com.backend.orbitflow.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 상점 아이템 (판매 중지는 삭제 대신 isOnSale = false, 이미 보유한 사용자는 계속 장착 가능)
@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "items")
public class Item extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemType type;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    private String imageUrl;

    @Column(nullable = false)
    private boolean isOnSale;

    public static Item of(String name, ItemType type, int price, String imageUrl) {
        return new Item(
                null, name, type, price, imageUrl, true
        );
    }

    public void updateItem(String name, ItemType type, int price) {
        this.name = name;
        this.type = type;
        this.price = price;
    }

    public void updateImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void updateOnSale(boolean isOnSale) {
        this.isOnSale = isOnSale;
    }
}
