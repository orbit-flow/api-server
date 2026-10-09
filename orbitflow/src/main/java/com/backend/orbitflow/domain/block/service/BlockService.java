package com.backend.orbitflow.domain.block.service;

import com.backend.orbitflow.domain.user.entity.User;

public interface BlockService {

    boolean isBlocked(User a, User b);
}
