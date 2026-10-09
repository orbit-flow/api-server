package com.backend.orbitflow.domain.timeline.controller;

import com.backend.orbitflow.domain.timeline.dto.TimelineResponse;
import com.backend.orbitflow.domain.timeline.dto.TimelineSuccessCode;
import com.backend.orbitflow.domain.timeline.facade.TimelineFacade;
import com.backend.orbitflow.global.common.dto.response.CommonResponse;
import com.backend.orbitflow.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/timeline")
public class TimelineController {

    private final TimelineFacade timelineFacade;

    // 팔로우한 계정의 투두 완료 활동과 투두 기반 게시글 (최신순, 커서 기반 무한 스크롤)
    // 첫 페이지는 cursor 없이 요청, 이후 응답의 nextCursor 전달
    @GetMapping
    public ResponseEntity<CommonResponse<TimelineResponse>> getTimeline(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        TimelineSuccessCode.GET_TIMELINE,
                        timelineFacade.getTimeline(authUser, cursor, size)
                ));
    }
}
