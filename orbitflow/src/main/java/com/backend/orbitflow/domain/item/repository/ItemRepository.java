package com.backend.orbitflow.domain.item.repository;

import com.backend.orbitflow.domain.item.entity.Item;
import com.backend.orbitflow.domain.item.enums.ItemType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemRepository extends JpaRepository<Item, Long> {

    // 상점 : 판매 중인 아이템 (type 미지정 시 전체 부위)
    @Query(value = """
            select i from Item i
            where i.isOnSale = true
              and (:type is null or i.type = :type)
            order by i.createdAt desc, i.id desc
            """,
            countQuery = """
            select count(i) from Item i
            where i.isOnSale = true
              and (:type is null or i.type = :type)
            """)
    Page<Item> findOnSale(@Param("type") ItemType type, Pageable pageable);

    // 관리자 : 부위·판매 여부 필터 (null이면 전체)
    @Query(value = """
            select i from Item i
            where (:type is null or i.type = :type)
              and (:onSale is null or i.isOnSale = :onSale)
            order by i.createdAt desc, i.id desc
            """,
            countQuery = """
            select count(i) from Item i
            where (:type is null or i.type = :type)
              and (:onSale is null or i.isOnSale = :onSale)
            """)
    Page<Item> search(@Param("type") ItemType type, @Param("onSale") Boolean onSale, Pageable pageable);
}
