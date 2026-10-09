package com.backend.orbitflow.domain.timeline.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// order : 같은 시각일 때의 정렬 순서 (커서 비교 키)
@Getter
@RequiredArgsConstructor
public enum TimelineItemType {

    POST(0),
    TODO_COMPLETED(1);

    private final int order;
}
