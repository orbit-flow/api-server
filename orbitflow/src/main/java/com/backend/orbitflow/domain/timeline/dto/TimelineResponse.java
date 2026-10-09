package com.backend.orbitflow.domain.timeline.dto;

import com.backend.orbitflow.domain.post.dto.response.PostResponse;
import com.backend.orbitflow.domain.timeline.enums.TimelineItemType;
import com.backend.orbitflow.domain.todo.dto.response.TodoResponse;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

// nextCursor : 다음 페이지 요청 시 전달 (hasNext = false면 null)
public record TimelineResponse(
        List<Item> items,
        String nextCursor,
        boolean hasNext
) {

    // type에 따라 post 또는 todo 중 하나만 포함
    public record Item(
            TimelineItemType type,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime occurredAt,
            Actor actor,
            PostResponse post,
            TodoResponse todo
    ) {
    }

    public record Actor(String uuid, String name, String profileImage) {
    }
}
