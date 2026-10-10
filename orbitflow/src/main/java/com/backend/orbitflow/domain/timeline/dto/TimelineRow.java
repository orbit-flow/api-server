package com.backend.orbitflow.domain.timeline.dto;

import java.time.LocalDateTime;

// 타임라인 네이티브 쿼리 결과 한 줄 (컬럼 별칭과 getter 이름이 일치해야 함)
// 개수·여부 컬럼은 MySQL이 정수로 반환하므로 Long으로 받아 TimelineResponse에서 변환
public interface TimelineRow {

    String getKind();
    Long getItemId();
    LocalDateTime getOccurredAt();
    String getActorUuid();
    String getActorName();
    String getActorProfileImage();
    Long getTodoId();
    String getTodoName();
    Long getTodoDeleted();
    Long getCategoryId();
    String getCategoryName();
    String getCategoryColor();
    String getContent();
    String getThumbnailUrl();
    Long getImageCount();
    Long getLikeCount();
    Long getCommentCount();
    Long getLiked();
}
