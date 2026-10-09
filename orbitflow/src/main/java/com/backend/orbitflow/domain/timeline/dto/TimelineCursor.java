package com.backend.orbitflow.domain.timeline.dto;

import com.backend.orbitflow.domain.timeline.error.TimelineErrorCode;
import com.backend.orbitflow.global.common.error.exception.CommonException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;

// 타임라인 정렬 키 (time desc, kind asc, id desc) 기준의 마지막 위치
// DATETIME이 초 단위라 같은 시각 항목이 많으므로 시각만으로는 페이지 경계에서 누락·중복이 생김
public record TimelineCursor(LocalDateTime time, int kind, long id) {

    // 첫 페이지 : 모든 항목보다 앞선 위치
    public static final TimelineCursor FIRST = new TimelineCursor(LocalDateTime.of(9999, 12, 31, 23, 59, 59), 0, Long.MAX_VALUE);

    // 이 커서가 other보다 정렬상 앞인지 (최신일수록 앞)
    public boolean isBefore(TimelineCursor other) {
        int byTime = time.compareTo(other.time);
        if (byTime != 0) {
            return byTime > 0;
        }
        if (kind != other.kind) {
            return kind < other.kind;
        }
        return id > other.id;
    }

    public String encode() {
        String raw = time + "|" + kind + "|" + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static TimelineCursor decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return FIRST;
        }
        try {
            String[] parts = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8).split("\\|");
            return new TimelineCursor(LocalDateTime.parse(parts[0]), Integer.parseInt(parts[1]), Long.parseLong(parts[2]));
        } catch (RuntimeException e) {
            throw new CommonException(TimelineErrorCode.INVALID_CURSOR);
        }
    }
}
