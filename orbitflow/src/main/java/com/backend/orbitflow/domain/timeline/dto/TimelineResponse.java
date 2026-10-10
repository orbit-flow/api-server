package com.backend.orbitflow.domain.timeline.dto;

import com.backend.orbitflow.domain.timeline.enums.TimelineItemType;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

// 타임라인 한 항목 (평면 구조, 목록 응답은 Page<TimelineResponse>)
// type이 TODO_COMPLETED면 게시글 필드(postId ~ liked)는 null·0
// 목록에서는 대표 사진과 사진 수만 제공하며, 사진 전체·댓글은 GET /api/posts/{postId}로 조회
public record TimelineResponse(
        TimelineItemType type,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime occurredAt,
        String actorUuid,
        String actorName,
        String actorProfileImage,
        Long todoId,
        String todoName,
        // 게시글에 연결된 투두가 삭제된 경우 true (게시글은 유지)
        boolean todoDeleted,
        Long categoryId,
        String categoryName,
        String categoryColor,
        Long postId,
        String content,
        String thumbnailUrl,
        int imageCount,
        long likeCount,
        long commentCount,
        boolean liked
) {

    public static TimelineResponse from(TimelineRow row) {
        boolean post = TimelineItemType.POST.name().equals(row.getKind());
        return new TimelineResponse(
                TimelineItemType.valueOf(row.getKind()),
                row.getOccurredAt(),
                row.getActorUuid(),
                row.getActorName(),
                row.getActorProfileImage(),
                row.getTodoId(),
                row.getTodoName(),
                isTrue(row.getTodoDeleted()),
                row.getCategoryId(),
                row.getCategoryName(),
                row.getCategoryColor(),
                post ? row.getItemId() : null,
                row.getContent(),
                row.getThumbnailUrl(),
                toInt(row.getImageCount()),
                toLong(row.getLikeCount()),
                toLong(row.getCommentCount()),
                isTrue(row.getLiked())
        );
    }

    private static boolean isTrue(Long value) {
        return value != null && value != 0;
    }

    private static int toInt(Long value) {
        return value == null ? 0 : value.intValue();
    }

    private static long toLong(Long value) {
        return value == null ? 0 : value;
    }
}
