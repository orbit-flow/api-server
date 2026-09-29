package com.backend.orbitflow.domain.user.facade;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import com.backend.orbitflow.domain.user.service.UserService;

@Component
@RequiredArgsConstructor
public class UserFacade {

    private final UserService UserService;

}
