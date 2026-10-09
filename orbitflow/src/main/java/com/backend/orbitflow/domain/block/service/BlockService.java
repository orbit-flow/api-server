package com.backend.orbitflow.domain.block.service;

import com.backend.orbitflow.domain.block.dto.response.BlockListResponse;
import com.backend.orbitflow.domain.block.entity.Block;
import com.backend.orbitflow.domain.user.entity.User;
import org.springframework.data.domain.Page;
import java.util.Collection;
import java.util.Set;

public interface BlockService {

    boolean isBlocked(User a, User b);
    Set<Long> findBlockedUserIdsAmong(User user, Collection<Long> userIds);
    Page<BlockListResponse> getBlockList(User blocker, int page, int size, String keyword);
    Block toggleBlock(User blocker, User blockee);
}
