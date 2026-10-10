package com.backend.orbitflow.domain.todo.error;

import com.backend.orbitflow.global.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TodoErrorCode implements ErrorCode {

    TODO_NOT_FOUND(HttpStatus.NOT_FOUND,
            "존재하지 않는 투두입니다.",
            "https://orbitflow.com/errors/todo-not-found",
            "Todo Not Found"),
    TODO_ACCESS_DENIED(HttpStatus.FORBIDDEN,
            "투두를 조회할 권한이 없습니다.",
            "https://orbitflow.com/errors/todo-access-denied",
            "Todo Access Denied"),
    TODO_EDIT_DENIED(HttpStatus.FORBIDDEN,
            "투두를 수정할 권한이 없습니다.",
            "https://orbitflow.com/errors/todo-edit-denied",
            "Todo Edit Denied"),
    INVALID_TODO_PERIOD(HttpStatus.BAD_REQUEST,
            "종료 시각은 시작 시각보다 빠를 수 없습니다.",
            "https://orbitflow.com/errors/invalid-todo-period",
            "Invalid Todo Period"),
    INVALID_PARENT_TODO(HttpStatus.BAD_REQUEST,
            "자식 투두는 같은 카테고리의 최상위 투두 아래에 한 단계까지만 만들 수 있습니다.",
            "https://orbitflow.com/errors/invalid-parent-todo",
            "Invalid Parent Todo"),
    INVALID_CHILD_ORDER(HttpStatus.BAD_REQUEST,
            "자식 투두 순서에는 모든 자식 투두가 정확히 한 번씩 포함되어야 합니다.",
            "https://orbitflow.com/errors/invalid-child-order",
            "Invalid Child Order"),
    NOT_TEAM_TODO(HttpStatus.BAD_REQUEST,
            "팀 투두만 담당자를 변경할 수 있습니다.",
            "https://orbitflow.com/errors/not-team-todo",
            "Not Team Todo"),
    RESTORE_EXPIRED(HttpStatus.BAD_REQUEST,
            "삭제 후 30일이 지난 투두는 복구할 수 없습니다.",
            "https://orbitflow.com/errors/restore-expired",
            "Restore Expired"),
    NOT_DELETED_TODO(HttpStatus.BAD_REQUEST,
            "삭제되지 않은 투두입니다.",
            "https://orbitflow.com/errors/not-deleted-todo",
            "Not Deleted Todo"),
    PARENT_TODO_DELETED(HttpStatus.BAD_REQUEST,
            "부모 투두가 삭제된 상태입니다. 부모 투두를 먼저 복구해주세요.",
            "https://orbitflow.com/errors/parent-todo-deleted",
            "Parent Todo Deleted"),

    // 반복
    ROUTINE_NOT_FOUND(HttpStatus.NOT_FOUND,
            "존재하지 않는 반복 일정입니다.",
            "https://orbitflow.com/errors/routine-not-found",
            "Routine Not Found"),
    CHILD_TODO_ROUTINE(HttpStatus.BAD_REQUEST,
            "자식 투두에는 반복을 설정할 수 없습니다.",
            "https://orbitflow.com/errors/child-todo-routine",
            "Child Todo Routine"),
    INVALID_DAYS_OF_WEEK(HttpStatus.BAD_REQUEST,
            "요일 값이 올바르지 않습니다. (MON=1 ~ SUN=64 비트 합, 1~127)",
            "https://orbitflow.com/errors/invalid-days-of-week",
            "Invalid Days Of Week"),
    NOT_ROUTINE_OCCURRENCE(HttpStatus.BAD_REQUEST,
            "반복 일정의 회차가 아닌 시각입니다.",
            "https://orbitflow.com/errors/not-routine-occurrence",
            "Not Routine Occurrence"),
    ROUTINE_SCOPE_REQUIRED(HttpStatus.BAD_REQUEST,
            "반복 일정을 변경할 범위(전체, 오늘부터)를 선택해주세요.",
            "https://orbitflow.com/errors/routine-scope-required",
            "Routine Scope Required"),
    OCCURRENCE_ALREADY_EXISTS(HttpStatus.CONFLICT,
            "이미 생성된(또는 삭제된) 회차입니다.",
            "https://orbitflow.com/errors/occurrence-already-exists",
            "Occurrence Already Exists");

    private final HttpStatus httpStatus;
    private final String message;
    private final String type;
    private final String title;
}
