package com.backend.orbitflow.domain.block.service;

import com.backend.orbitflow.domain.block.dto.response.BlockListResponse;
import com.backend.orbitflow.domain.block.entity.Block;
import com.backend.orbitflow.domain.block.repository.BlockRepository;
import com.backend.orbitflow.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Collection;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class BlockServiceImpl implements BlockService{

    private final BlockRepository blockRepository;

    @Transactional(readOnly = true)
    public Set<Long> findBlockedUserIdsAmong(User user, Collection<Long> userIds) {
        return userIds.isEmpty() ? Set.of() : blockRepository.findBlockedUserIdsAmong(user, userIds);
    }

    @Transactional(readOnly = true)
    public boolean isBlocked(User a, User b) {
        return blockRepository.existsBetween(a, b);
    }

    // 요청 page는 1부터 시작
    @Transactional(readOnly = true)
    public Page<BlockListResponse> getBlockList(User blocker, int page, int size, String keyword) {
        return blockRepository.findBlocks(blocker, keyword, PageRequest.of(Math.max(page - 1, 0), size));
    }

    // 내가 차단한 기록이 있으면 해제 후 null 반환, 없으면 차단 생성
    // 상대가 나를 차단한 기록은 별개이므로 영향을 주지 않음
    public Block toggleBlock(User blocker, User blockee) {
        Optional<Block> block = blockRepository.findByBlockerAndBlockee(blocker, blockee);
        if (block.isPresent()) {
            blockRepository.delete(block.get());
            return null;
        }
        return blockRepository.save(Block.of(blocker, blockee));
    }
}
