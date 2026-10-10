package com.backend.orbitflow.domain.timeline.facade;

import com.backend.orbitflow.domain.timeline.dto.TimelineResponse;
import com.backend.orbitflow.domain.timeline.service.TimelineService;
import com.backend.orbitflow.domain.user.service.UserService;
import com.backend.orbitflow.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.backend.orbitflow.global.common.dto.response.PageResponse;

@Component
@RequiredArgsConstructor
public class TimelineFacade {

    private final TimelineService timelineService;
    private final UserService userService;

    @Transactional(readOnly = true)
    public PageResponse<TimelineResponse> getTimeline(AuthUser authUser, int page, int size) {
        return PageResponse.from(timelineService.getTimeline(userService.getByUuid(authUser.getUuid()), page, size));
    }
}
