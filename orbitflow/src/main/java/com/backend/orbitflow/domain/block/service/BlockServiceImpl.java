package com.backend.orbitflow.domain.block.service;

import com.backend.orbitflow.domain.block.repository.BlockRepository;
import com.backend.orbitflow.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class BlockServiceImpl implements BlockService{

    private final BlockRepository blockRepository;

    @Transactional(readOnly = true)
    public boolean isBlocked(User a, User b) {
        return blockRepository.existsBetween(a, b);
    }
}
