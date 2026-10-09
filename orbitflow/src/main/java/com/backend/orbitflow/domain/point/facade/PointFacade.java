package com.backend.orbitflow.domain.point.facade;

import com.backend.orbitflow.domain.point.dto.response.AttendanceStatusResponse;
import com.backend.orbitflow.domain.point.dto.response.PointTransactionResponse;
import com.backend.orbitflow.domain.point.service.PointService;
import com.backend.orbitflow.domain.user.service.UserService;
import com.backend.orbitflow.global.common.dto.response.PageResponse;
import com.backend.orbitflow.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PointFacade {

    private final PointService pointService;
    private final UserService userService;

    @Transactional
    public PointTransactionResponse attend(AuthUser authUser) {
        return pointService.attend(userService.getByUuid(authUser.getUuid()));
    }

    @Transactional(readOnly = true)
    public AttendanceStatusResponse getAttendanceStatus(AuthUser authUser) {
        return pointService.getAttendanceStatus(userService.getByUuid(authUser.getUuid()));
    }

    @Transactional(readOnly = true)
    public PageResponse<PointTransactionResponse> getHistory(AuthUser authUser, int page, int size) {
        return PageResponse.from(pointService.getHistory(userService.getByUuid(authUser.getUuid()), page, size));
    }
}
