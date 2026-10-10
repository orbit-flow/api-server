package com.backend.orbitflow.domain.todo.enums;

// 반복 일정 변경·해제 범위
// ALL : 전체 (과거 회차는 유지하고 규칙 자체를 변경), FROM_TODAY : 오늘부터 (오늘 이전 회차는 기존 규칙 유지)
public enum RoutineScope {
    ALL, FROM_TODAY
}
