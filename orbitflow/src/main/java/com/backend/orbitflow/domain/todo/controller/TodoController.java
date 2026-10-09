package com.backend.orbitflow.domain.todo.controller;

import com.backend.orbitflow.domain.todo.dto.request.RoutineOccurrenceRequest;
import com.backend.orbitflow.domain.todo.dto.request.RoutineRequest;
import com.backend.orbitflow.domain.todo.dto.request.TodoAssigneeRequest;
import com.backend.orbitflow.domain.todo.dto.request.TodoCreateRequest;
import com.backend.orbitflow.domain.todo.dto.request.TodoOrderRequest;
import com.backend.orbitflow.domain.todo.dto.request.TodoUpdateRequest;
import com.backend.orbitflow.domain.todo.dto.response.DashboardResponse;
import com.backend.orbitflow.domain.todo.dto.response.RoutineResponse;
import com.backend.orbitflow.domain.todo.dto.response.TodoResponse;
import com.backend.orbitflow.domain.todo.dto.response.TodoSuccessCode;
import com.backend.orbitflow.domain.todo.facade.TodoFacade;
import com.backend.orbitflow.global.common.dto.response.CommonResponse;
import com.backend.orbitflow.global.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class TodoController {

    private final TodoFacade todoFacade;

    // ---------- 카테고리 단위 ----------

    @PostMapping("/categories/{categoryId}/todos")
    public ResponseEntity<CommonResponse<TodoResponse>> createTodo(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long categoryId,
            @Valid @RequestBody TodoCreateRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CommonResponse.success(
                        TodoSuccessCode.TODO_CREATE,
                        todoFacade.createTodo(authUser, categoryId, request)
                ));
    }

    @GetMapping("/categories/{categoryId}/todos")
    public ResponseEntity<CommonResponse<List<TodoResponse>>> getCategoryTodos(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long categoryId
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        TodoSuccessCode.GET_TODO_LIST,
                        todoFacade.getCategoryTodos(authUser, categoryId)
                ));
    }

    // 복구 가능한(삭제 후 30일 이내) 투두
    @GetMapping("/categories/{categoryId}/todos/deleted")
    public ResponseEntity<CommonResponse<List<TodoResponse>>> getRestorableTodos(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long categoryId
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        TodoSuccessCode.GET_DELETED_TODO_LIST,
                        todoFacade.getRestorableTodos(authUser, categoryId)
                ));
    }

    // ---------- 개인 대시보드 ----------

    // date 미지정 시 오늘
    @GetMapping("/todos/dashboard")
    public ResponseEntity<CommonResponse<DashboardResponse>> getDashboard(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        TodoSuccessCode.GET_DASHBOARD,
                        todoFacade.getDashboard(authUser, date)
                ));
    }

    // ---------- 투두 단위 ----------

    @GetMapping("/todos/{todoId}")
    public ResponseEntity<CommonResponse<TodoResponse>> getTodo(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long todoId
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        TodoSuccessCode.GET_TODO_INFO,
                        todoFacade.getTodo(authUser, todoId)
                ));
    }

    @PutMapping("/todos/{todoId}")
    public ResponseEntity<CommonResponse<TodoResponse>> updateTodo(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long todoId,
            @Valid @RequestBody TodoUpdateRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        TodoSuccessCode.TODO_UPDATE,
                        todoFacade.updateTodo(authUser, todoId, request)
                ));
    }

    @PatchMapping("/todos/{todoId}/complete")
    public ResponseEntity<CommonResponse<TodoResponse>> toggleComplete(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long todoId
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        TodoSuccessCode.TODO_COMPLETE_TOGGLE,
                        todoFacade.toggleComplete(authUser, todoId)
                ));
    }

    // 팀 투두 담당자 배정
    @PatchMapping("/todos/{todoId}/assignee")
    public ResponseEntity<CommonResponse<TodoResponse>> updateAssignee(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long todoId,
            @Valid @RequestBody TodoAssigneeRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        TodoSuccessCode.TODO_ASSIGNEE_UPDATE,
                        todoFacade.updateAssignee(authUser, todoId, request)
                ));
    }

    // 자식 투두 표시 순서 변경
    @PatchMapping("/todos/{todoId}/children/order")
    public ResponseEntity<CommonResponse<List<TodoResponse>>> updateChildOrder(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long todoId,
            @Valid @RequestBody TodoOrderRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        TodoSuccessCode.TODO_CHILD_ORDER_UPDATE,
                        todoFacade.updateChildOrder(authUser, todoId, request)
                ));
    }

    @DeleteMapping("/todos/{todoId}")
    public ResponseEntity<CommonResponse<Void>> deleteTodo(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long todoId
    ) {
        todoFacade.deleteTodo(authUser, todoId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        TodoSuccessCode.TODO_DELETE
                ));
    }

    @PostMapping("/todos/{todoId}/restore")
    public ResponseEntity<CommonResponse<TodoResponse>> restoreTodo(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long todoId
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        TodoSuccessCode.TODO_RESTORE,
                        todoFacade.restoreTodo(authUser, todoId)
                ));
    }

    // ---------- 반복 ----------

    // 반복 설정 (이미 반복 중이면 규칙 변경, 아직 생성되지 않은 회차부터 적용)
    @PutMapping("/todos/{todoId}/routine")
    public ResponseEntity<CommonResponse<RoutineResponse>> upsertRoutine(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long todoId,
            @Valid @RequestBody RoutineRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        TodoSuccessCode.ROUTINE_UPDATE,
                        todoFacade.upsertRoutine(authUser, todoId, request)
                ));
    }

    // 반복 해제 (생성된 회차는 일반 투두로 유지)
    @DeleteMapping("/todos/{todoId}/routine")
    public ResponseEntity<CommonResponse<Void>> deleteRoutine(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long todoId
    ) {
        todoFacade.deleteRoutine(authUser, todoId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        TodoSuccessCode.ROUTINE_DELETE
                ));
    }

    // 미리보기 회차 즉시 생성
    @PostMapping("/routines/{routineId}/occurrences")
    public ResponseEntity<CommonResponse<TodoResponse>> createOccurrence(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long routineId,
            @Valid @RequestBody RoutineOccurrenceRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CommonResponse.success(
                        TodoSuccessCode.OCCURRENCE_CREATE,
                        todoFacade.createOccurrence(authUser, routineId, request)
                ));
    }
}
