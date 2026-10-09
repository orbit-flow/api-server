package com.backend.orbitflow.domain.timeline.facade;

import com.backend.orbitflow.domain.timeline.dto.TimelineResponse;
import com.backend.orbitflow.domain.timeline.service.TimelineService;
import com.backend.orbitflow.domain.user.service.UserService;
import com.backend.orbitflow.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class TimelineFacade {

    private final TimelineService timelineService;
    private final UserService userService;

    @Transactional(readOnly = true)
    public TimelineResponse getTimeline(AuthUser authUser, String cursor, int size) {
        return timelineService.getTimeline(userService.getByUuid(authUser.getUuid()), cursor, size);
    }
}
