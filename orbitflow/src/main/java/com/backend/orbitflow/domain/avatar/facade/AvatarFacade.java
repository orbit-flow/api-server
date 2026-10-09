package com.backend.orbitflow.domain.avatar.facade;

import com.backend.orbitflow.domain.avatar.dto.response.AvatarResponse;
import com.backend.orbitflow.domain.avatar.dto.response.PurchaseResponse;
import com.backend.orbitflow.domain.avatar.dto.response.UserItemResponse;
import com.backend.orbitflow.domain.avatar.service.AvatarService;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.domain.user.service.UserService;
import com.backend.orbitflow.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AvatarFacade {

    private final AvatarService avatarService;
    private final UserService userService;

    @Transactional
    public AvatarResponse getMyAvatar(AuthUser authUser) {
        return avatarService.getMyAvatar(me(authUser));
    }

    @Transactional(readOnly = true)
    public AvatarResponse getUserAvatar(String userUuid) {
        return avatarService.getUserAvatar(userService.getByUuid(userUuid));
    }

    @Transactional
    public List<UserItemResponse> getMyItems(AuthUser authUser) {
        return avatarService.getMyItems(me(authUser));
    }

    @Transactional
    public PurchaseResponse purchase(AuthUser authUser, Long itemId) {
        return avatarService.purchase(me(authUser), itemId);
    }

    @Transactional
    public UserItemResponse equip(AuthUser authUser, Long itemId) {
        return avatarService.equip(me(authUser), itemId);
    }

    @Transactional
    public UserItemResponse unequip(AuthUser authUser, Long itemId) {
        return avatarService.unequip(me(authUser), itemId);
    }

    private User me(AuthUser authUser) {
        return userService.getByUuid(authUser.getUuid());
    }
}
