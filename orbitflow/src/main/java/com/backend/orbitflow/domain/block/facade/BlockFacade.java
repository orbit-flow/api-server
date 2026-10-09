package com.backend.orbitflow.domain.block.facade;

import com.backend.orbitflow.domain.block.dto.response.BlockListResponse;
import com.backend.orbitflow.domain.block.dto.response.BlockResponse;
import com.backend.orbitflow.domain.block.error.BlockErrorCode;
import com.backend.orbitflow.domain.block.service.BlockService;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.domain.user.service.UserService;
import com.backend.orbitflow.global.common.dto.response.PageResponse;
import com.backend.orbitflow.global.common.error.exception.CommonException;
import com.backend.orbitflow.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BlockFacade {

    private final BlockService blockService;
    private final UserService userService;

//    public PageResponse<BlockListResponse> getBlockList(AuthUser authUser, int page, int size, String keyword) {
//        return PageResponse.from(
//                blockService.getBlockList(
//                        userService.getByUuid(authUser.getUuid()),
//                        page,
//                        size,
//                        keyword
//                )
//        );
//    }
//
//    public BlockResponse blockToggle(AuthUser authUser, String uuid) {
//        if (authUser.getUuid().equals(uuid)){
//            throw new CommonException(BlockErrorCode.SELF_BLOCK);
//        }
//        User requestUser = userService.getByUuid(authUser.getUuid());
//        User targetUser = userService.getByUuid(uuid);
//
//    }
}
