package com.backend.orbitflow.domain.avatar.service;

import com.backend.orbitflow.domain.avatar.dto.response.AvatarResponse;
import com.backend.orbitflow.domain.avatar.dto.response.PurchaseResponse;
import com.backend.orbitflow.domain.avatar.dto.response.UserItemResponse;
import com.backend.orbitflow.domain.avatar.entity.Avatar;
import com.backend.orbitflow.domain.avatar.entity.UserItem;
import com.backend.orbitflow.domain.avatar.error.AvatarErrorCode;
import com.backend.orbitflow.domain.avatar.repository.AvatarRepository;
import com.backend.orbitflow.domain.avatar.repository.UserItemRepository;
import com.backend.orbitflow.domain.item.entity.Item;
import com.backend.orbitflow.domain.item.error.ItemErrorCode;
import com.backend.orbitflow.domain.item.repository.ItemRepository;
import com.backend.orbitflow.domain.notification.event.ItemPurchasedEvent;
import com.backend.orbitflow.domain.point.entity.PointTransaction;
import com.backend.orbitflow.domain.point.enums.PointTransactionType;
import com.backend.orbitflow.domain.point.service.PointLedger;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.global.common.error.exception.CommonException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class AvatarServiceImpl implements AvatarService {

    private final AvatarRepository avatarRepository;
    private final UserItemRepository userItemRepository;
    private final ItemRepository itemRepository;
    private final PointLedger pointLedger;
    private final ApplicationEventPublisher eventPublisher;

    // 가입 시 기본 아바타와 초기 포인트 지급
    public Avatar createAvatar(User user) {
        return pointLedger.createAvatar(user);
    }

    public AvatarResponse getMyAvatar(User me) {
        Avatar avatar = getOrCreate(me);
        return AvatarResponse.of(avatar, me, userItemRepository.findEquippedByAvatar(avatar), true);
    }

    // 다른 사용자의 아바타는 장착 아이템만 공개 (포인트 비공개)
    @Transactional(readOnly = true)
    public AvatarResponse getUserAvatar(User target) {
        return avatarRepository.findByUser(target)
                .map(avatar -> AvatarResponse.of(avatar, target, userItemRepository.findEquippedByAvatar(avatar), false))
                .orElseGet(() -> AvatarResponse.of(Avatar.of(target), target, List.of(), false));
    }

    public List<UserItemResponse> getMyItems(User me) {
        return userItemRepository.findAllWithItemByAvatar(getOrCreate(me)).stream()
                .map(UserItemResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Set<Long> findOwnedItemIds(User me, Collection<Item> items) {
        if (items.isEmpty()) {
            return Set.of();
        }
        return avatarRepository.findByUser(me)
                .map(avatar -> userItemRepository.findOwnedItemIds(avatar, items))
                .orElse(Set.of());
    }

    // 판매 중인 아이템을 가격 이상의 포인트를 보유한 경우에만 구매, 구매 즉시 보유 목록에 추가 (취소·환급 불가)
    // 아바타 행 락을 먼저 잡고 보유·잔액을 확인하므로 동시 구매(같은 아이템 중복, 잔액 초과)가 직렬화됨
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PurchaseResponse purchase(User me, Long itemId) {
        Avatar avatar = pointLedger.lock(me);
        Item item = itemRepository.findById(itemId).orElseThrow(
                () -> new CommonException(ItemErrorCode.ITEM_NOT_FOUND)
        );
        if (!item.isOnSale()) {
            throw new CommonException(AvatarErrorCode.ITEM_NOT_ON_SALE);
        }
        if (userItemRepository.existsByAvatarAndItem(avatar, item)) {
            throw new CommonException(AvatarErrorCode.ALREADY_OWNED_ITEM);
        }
        int price = item.getPrice();
        PointTransaction transaction = pointLedger.withdraw(me, PointTransactionType.USE, price);
        userItemRepository.save(UserItem.of(avatar, item));
        eventPublisher.publishEvent(new ItemPurchasedEvent(me.getId(), item.getId(), price, transaction.getBalanceAfter()));
        return new PurchaseResponse(item.getId(), item.getName(), price, transaction.getBalanceAfter());
    }

    // 같은 부위에 장착된 아이템은 즉시 해제하고 새 아이템 장착
    public UserItemResponse equip(User me, Long itemId) {
        Avatar avatar = getOrCreate(me);
        UserItem userItem = getOwnedItem(avatar, itemId);
        userItemRepository.findEquippedByAvatarAndType(avatar, userItem.getItem().getType()).stream()
                .filter(equipped -> !equipped.getId().equals(userItem.getId()))
                .forEach(UserItem::unequip);
        userItem.equip();
        return UserItemResponse.from(userItem);
    }

    public UserItemResponse unequip(User me, Long itemId) {
        UserItem userItem = getOwnedItem(getOrCreate(me), itemId);
        userItem.unequip();
        return UserItemResponse.from(userItem);
    }

    private Avatar getOrCreate(User user) {
        return avatarRepository.findByUser(user).orElseGet(() -> createAvatar(user));
    }

    private UserItem getOwnedItem(Avatar avatar, Long itemId) {
        return userItemRepository.findByAvatarAndItemId(avatar, itemId).orElseThrow(
                () -> new CommonException(AvatarErrorCode.NOT_OWNED_ITEM)
        );
    }
}
