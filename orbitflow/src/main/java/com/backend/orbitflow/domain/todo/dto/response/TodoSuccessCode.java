package com.backend.orbitflow.domain.todo.dto.response;

import com.backend.orbitflow.global.common.dto.response.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TodoSuccessCode implements SuccessCode {

    TODO_CREATE(HttpStatus.CREATED, "투두가 생성되었습니다."),
    GET_TODO_LIST(HttpStatus.OK, "투두 리스트가 열람되었습니다."),
    GET_TODO_INFO(HttpStatus.OK, "투두 정보가 열람되었습니다."),
    GET_DASHBOARD(HttpStatus.OK, "대시보드가 열람되었습니다."),
    TODO_UPDATE(HttpStatus.OK, "투두가 업데이트 되었습니다."),
    TODO_COMPLETE_TOGGLE(HttpStatus.OK, "투두 완료 상태가 변경되었습니다."),
    TODO_ASSIGNEE_UPDATE(HttpStatus.OK, "투두 담당자가 변경되었습니다."),
    TODO_CHILD_ORDER_UPDATE(HttpStatus.OK, "자식 투두 순서가 변경되었습니다."),
    TODO_DELETE(HttpStatus.OK, "투두가 삭제되었습니다."),
    GET_DELETED_TODO_LIST(HttpStatus.OK, "복구 가능한 투두 리스트가 열람되었습니다."),
    TODO_RESTORE(HttpStatus.OK, "투두가 복구되었습니다."),

    // 반복
    ROUTINE_UPDATE(HttpStatus.OK, "반복 일정이 설정되었습니다."),
    ROUTINE_DELETE(HttpStatus.OK, "반복 일정이 해제되었습니다."),
    OCCURRENCE_CREATE(HttpStatus.CREATED, "반복 회차가 생성되었습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
