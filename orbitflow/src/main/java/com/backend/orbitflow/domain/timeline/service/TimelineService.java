package com.backend.orbitflow.domain.timeline.service;

import com.backend.orbitflow.domain.timeline.dto.TimelineResponse;
import com.backend.orbitflow.domain.timeline.repository.TimelineRepository;
import com.backend.orbitflow.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 타임라인 : 팔로우한 계정의 투두 기반 게시글과 투두 완료 활동 (최신순 페이지)
// 요청당 쿼리 수 고정 : 목록 1회 + 전체 개수 1회 (TimelineRepository 참고)
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TimelineService {

    public static final int MAX_SIZE = 50;

    private final TimelineRepository timelineRepository;

    // 요청 page는 1부터 시작
    public Page<TimelineResponse> getTimeline(User viewer, int page, int size) {
        return timelineRepository.findTimeline(
                viewer.getId(),
                PageRequest.of(Math.max(page - 1, 0), Math.min(Math.max(size, 1), MAX_SIZE))
        );
    }
}
