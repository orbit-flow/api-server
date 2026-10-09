package com.backend.orbitflow.domain.todo.entity;

import com.backend.orbitflow.domain.todo.enums.DurationType;
import com.backend.orbitflow.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

// 반복 규칙, todo는 반복의 기준이 되는 원본 투두
@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "routines")
public class Routine extends BaseEntity {

    // 잘못된 규칙으로 인한 무한 반복 방지
    private static final int MAX_ITERATIONS = 10_000;

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "todo_id", nullable = false, unique = true)
    private Todo todo;

    // 반복 간격 (duration_type 단위)
    @Column(nullable = false)
    private int duration;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DurationType durationType;

    // WEEK 전용 요일 비트마스크 : MON=1, TUE=2, WED=4, THU=8, FRI=16, SAT=32, SUN=64
    private Integer daysOfWeek;

    // null이면 종료일 없음
    private LocalDateTime repeatEndDate;

    public static Routine of(Todo todo, int duration, DurationType durationType, Integer daysOfWeek, LocalDateTime repeatEndDate) {
        return new Routine(
                null, todo, duration, durationType, daysOfWeek, repeatEndDate
        );
    }

    public void updateRule(int duration, DurationType durationType, Integer daysOfWeek, LocalDateTime repeatEndDate) {
        this.duration = duration;
        this.durationType = durationType;
        this.daysOfWeek = daysOfWeek;
        this.repeatEndDate = repeatEndDate;
    }

    public boolean isOccurrence(LocalDateTime startDate) {
        return occurrencesBetween(startDate, startDate).contains(startDate);
    }

    // [from, to] 구간에 시작하는 회차 시작 시각 목록 (원본 투두 시작 시각 기준, 원본 회차 포함)
    public List<LocalDateTime> occurrencesBetween(LocalDateTime from, LocalDateTime to) {
        LocalDateTime origin = todo.getStartDate();
        LocalDateTime end = repeatEndDate != null && repeatEndDate.isBefore(to) ? repeatEndDate : to;
        List<LocalDateTime> result = new ArrayList<>();
        if (end.isBefore(from) || end.isBefore(origin)) {
            return result;
        }
        if (durationType == DurationType.WEEK) {
            return weeklyOccurrences(origin, from, end);
        }
        for (int k = 0; k < MAX_ITERATIONS; k++) {
            LocalDateTime candidate = nth(origin, (long) k * duration);
            if (candidate.isAfter(end)) {
                break;
            }
            if (!candidate.isBefore(from)) {
                result.add(candidate);
            }
        }
        return result;
    }

    private LocalDateTime nth(LocalDateTime origin, long step) {
        return switch (durationType) {
            case DAY -> origin.plusDays(step);
            case MONTH -> origin.plusMonths(step);
            case MONTH_END -> origin.withDayOfMonth(1).plusMonths(step).with(TemporalAdjusters.lastDayOfMonth());
            case YEAR -> origin.plusYears(step);
            case WEEK -> throw new IllegalStateException("WEEK은 weeklyOccurrences에서 처리");
        };
    }

    // duration주 간격으로 지정 요일 반복, 요일 미지정 시 원본 투두의 요일
    private List<LocalDateTime> weeklyOccurrences(LocalDateTime origin, LocalDateTime from, LocalDateTime end) {
        int mask = daysOfWeek == null || daysOfWeek == 0 ? dayBit(origin.getDayOfWeek()) : daysOfWeek;
        LocalDate firstWeek = origin.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<LocalDateTime> result = new ArrayList<>();
        for (int k = 0; k < MAX_ITERATIONS; k++) {
            LocalDate week = firstWeek.plusWeeks((long) k * duration);
            if (week.atTime(origin.toLocalTime()).isAfter(end)) {
                break;
            }
            for (DayOfWeek day : DayOfWeek.values()) {
                if ((mask & dayBit(day)) == 0) {
                    continue;
                }
                LocalDateTime candidate = week.plusDays(day.getValue() - 1).atTime(origin.toLocalTime());
                if (!candidate.isBefore(origin) && !candidate.isBefore(from) && !candidate.isAfter(end)) {
                    result.add(candidate);
                }
            }
        }
        return result;
    }

    private static int dayBit(DayOfWeek day) {
        return 1 << (day.getValue() - 1);
    }
}
