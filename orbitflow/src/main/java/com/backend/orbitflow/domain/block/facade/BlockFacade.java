package com.backend.orbitflow.domain.block.facade;

import com.backend.orbitflow.domain.block.dto.response.BlockListResponse;
import com.backend.orbitflow.domain.block.dto.response.BlockResponse;
import com.backend.orbitflow.domain.block.entity.Block;
import com.backend.orbitflow.domain.block.error.BlockErrorCode;
import com.backend.orbitflow.domain.block.service.BlockService;
import com.backend.orbitflow.domain.follow.service.FollowService;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.domain.user.service.UserService;
import com.backend.orbitflow.global.common.dto.response.PageResponse;
import com.backend.orbitflow.global.common.error.exception.CommonException;
import com.backend.orbitflow.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class BlockFacade {

    private final BlockService blockService;
    private final UserService userService;
    private final FollowService followService;

    public PageResponse<BlockListResponse> getBlockList(AuthUser authUser, int page, int size, String keyword) {
        return PageResponse.from(
                blockService.getBlockList(
                        userService.getByUuid(authUser.getUuid()),
                        page,
                        size,
                        keyword
                )
        );
    }

    // 차단 시 양방향 팔로우·팔로우 요청을 함께 삭제 (차단 해제 시 복구하지 않음)
    @Transactional
    public BlockResponse toggleBlock(AuthUser authUser, String uuid) {
        if (authUser.getUuid().equals(uuid)) {
            throw new CommonException(BlockErrorCode.SELF_BLOCK);
        }
        User requestUser = userService.getByUuid(authUser.getUuid());
        User targetUser = userService.getByUuid(uuid);

        Block block = blockService.toggleBlock(requestUser, targetUser);
        if (block == null) {
            return BlockResponse.unblocked(targetUser.getUuid());
        }
        followService.deleteAllBetween(requestUser, targetUser);
        return BlockResponse.of(block.getId(), targetUser.getUuid(), block.getCreatedAt());
    }
}
