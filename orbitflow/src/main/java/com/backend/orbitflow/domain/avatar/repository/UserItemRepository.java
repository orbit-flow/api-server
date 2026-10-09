package com.backend.orbitflow.domain.avatar.repository;

import com.backend.orbitflow.domain.avatar.entity.Avatar;
import com.backend.orbitflow.domain.avatar.entity.UserItem;
import com.backend.orbitflow.domain.item.entity.Item;
import com.backend.orbitflow.domain.item.enums.ItemType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserItemRepository extends JpaRepository<UserItem, Long> {

    boolean existsByAvatarAndItem(Avatar avatar, Item item);

    @Query("select ui from UserItem ui join fetch ui.item where ui.avatar = :avatar and ui.item.id = :itemId")
    Optional<UserItem> findByAvatarAndItemId(@Param("avatar") Avatar avatar, @Param("itemId") Long itemId);

    @Query("select ui from UserItem ui join fetch ui.item where ui.avatar = :avatar order by ui.createdAt desc")
    List<UserItem> findAllWithItemByAvatar(@Param("avatar") Avatar avatar);

    @Query("select ui from UserItem ui join fetch ui.item where ui.avatar = :avatar and ui.isEquipped = true")
    List<UserItem> findEquippedByAvatar(@Param("avatar") Avatar avatar);

    // 같은 부위에 장착된 아이템 (부위당 1개)
    @Query("""
            select ui from UserItem ui
            join ui.item i
            where ui.avatar = :avatar
              and ui.isEquipped = true
              and i.type = :type
            """)
    List<UserItem> findEquippedByAvatarAndType(@Param("avatar") Avatar avatar, @Param("type") ItemType type);

    // 아이템 장착 부위 변경 시 기존 장착 해제 (부위당 1개 장착 유지)
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update UserItem ui set ui.isEquipped = false where ui.item = :item and ui.isEquipped = true")
    void unequipAllByItem(@Param("item") Item item);

    @Query("select ui.item.id from UserItem ui where ui.avatar = :avatar and ui.item in :items")
    Set<Long> findOwnedItemIds(@Param("avatar") Avatar avatar, @Param("items") Collection<Item> items);
}
