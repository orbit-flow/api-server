package com.backend.orbitflow.domain.avatar.facade;

import com.backend.orbitflow.domain.avatar.dto.response.AvatarResponse;
import com.backend.orbitflow.domain.avatar.dto.response.PurchaseResponse;
import com.backend.orbitflow.domain.avatar.dto.response.UserItemResponse;
import com.backend.orbitflow.domain.avatar.service.AvatarService;
import com.backend.orbitflow.domain.notification.enums.NotificationType;
import com.backend.orbitflow.domain.notification.event.NotificationRequest;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.domain.user.service.UserService;
import com.backend.orbitflow.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.backend.orbitflow.global.common.dto.response.PageResponse;

@Component
@RequiredArgsConstructor
public class AvatarFacade {

    private final AvatarService avatarService;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public AvatarResponse getMyAvatar(AuthUser authUser) {
        return avatarService.getMyAvatar(me(authUser));
    }

    @Transactional(readOnly = true)
    public AvatarResponse getUserAvatar(String userUuid) {
        return avatarService.getUserAvatar(userService.getByUuid(userUuid));
    }

    @Transactional(readOnly = true)
    public PageResponse<UserItemResponse> getMyItems(AuthUser authUser, int page, int size) {
        return PageResponse.from(avatarService.getMyItems(me(authUser), page, size));
    }

    // 포인트 변경 : READ COMMITTED 필수 (PointLedger 참고), 구매 결과는 처리 완료 즉시 앱 내 알림으로 고지
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PurchaseResponse purchase(AuthUser authUser, Long itemId) {
        User me = me(authUser);
        PurchaseResponse response = avatarService.purchase(me, itemId);
        eventPublisher.publishEvent(NotificationRequest.to(me, NotificationType.ITEM_PURCHASED, null, response.itemId(), null,
                "'" + response.name() + "'을(를) " + response.price() + "P에 구매했습니다. (잔액 " + response.balanceAfter() + "P)"));
        return response;
    }

    // 장착 변경 : 아바타 행 락 + READ COMMITTED (같은 부위 동시 장착 방지)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public UserItemResponse equip(AuthUser authUser, Long itemId) {
        return avatarService.equip(me(authUser), itemId);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public UserItemResponse unequip(AuthUser authUser, Long itemId) {
        return avatarService.unequip(me(authUser), itemId);
    }

    private User me(AuthUser authUser) {
        return userService.getByUuid(authUser.getUuid());
    }
}
