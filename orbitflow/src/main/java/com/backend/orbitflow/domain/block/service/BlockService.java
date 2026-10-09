package com.backend.orbitflow.domain.block.service;

import com.backend.orbitflow.domain.block.dto.response.BlockListResponse;
import com.backend.orbitflow.domain.block.entity.Block;
import com.backend.orbitflow.domain.user.entity.User;
import org.springframework.data.domain.Page;

public interface BlockService {

    boolean isBlocked(User a, User b);
    Page<BlockListResponse> getBlockList(User blocker, int page, int size, String keyword);
    Block toggleBlock(User blocker, User blockee);
}
