package com.backend.orbitflow.domain.todo.controller;

import com.backend.orbitflow.domain.todo.dto.request.RoutineOccurrenceRequest;
import com.backend.orbitflow.domain.todo.dto.request.RoutineRequest;
import com.backend.orbitflow.domain.todo.dto.request.TodoAssigneeRequest;
import com.backend.orbitflow.domain.todo.dto.request.TodoCreateRequest;
import com.backend.orbitflow.domain.todo.dto.request.TodoOrderRequest;
import com.backend.orbitflow.domain.todo.dto.request.TodoUpdateRequest;
import com.backend.orbitflow.domain.todo.dto.response.RoutineResponse;
import com.backend.orbitflow.domain.todo.dto.response.TodoResponse;
import com.backend.orbitflow.domain.todo.dto.response.TodoSuccessCode;
import com.backend.orbitflow.domain.todo.enums.RoutineScope;
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
import com.backend.orbitflow.domain.todo.dto.response.DashboardTodoResponse;
import com.backend.orbitflow.global.common.dto.response.PageResponse;

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
    public ResponseEntity<CommonResponse<PageResponse<TodoResponse>>> getCategoryTodos(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        TodoSuccessCode.GET_TODO_LIST,
                        todoFacade.getCategoryTodos(authUser, categoryId, page, size)
                ));
    }

    // 복구 가능한(삭제 후 30일 이내) 투두
    @GetMapping("/categories/{categoryId}/todos/deleted")
    public ResponseEntity<CommonResponse<PageResponse<TodoResponse>>> getRestorableTodos(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        TodoSuccessCode.GET_DELETED_TODO_LIST,
                        todoFacade.getRestorableTodos(authUser, categoryId, page, size)
                ));
    }

    // ---------- 개인 대시보드 ----------

    // 대시보드는 칸반별로 나눠 각각 페이지 조회 (date 미지정 시 오늘)
    // 일정 칸반 : 시작 시각순
    @GetMapping("/todos/dashboard/schedules")
    public ResponseEntity<CommonResponse<PageResponse<DashboardTodoResponse>>> getDashboardSchedules(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        TodoSuccessCode.GET_DASHBOARD,
                        todoFacade.getDashboardSchedules(authUser, date, page, size)
                ));
    }

    // 백로그 칸반 : 카테고리 생성순 → 시작 시각순
    @GetMapping("/todos/dashboard/backlogs")
    public ResponseEntity<CommonResponse<PageResponse<DashboardTodoResponse>>> getDashboardBacklogs(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        TodoSuccessCode.GET_DASHBOARD,
                        todoFacade.getDashboardBacklogs(authUser, date, page, size)
                ));
    }

    // 아직 생성되지 않은 반복 회차 (작업 시 POST /api/routines/{routineId}/occurrences 로 생성)
    @GetMapping("/todos/dashboard/previews")
    public ResponseEntity<CommonResponse<PageResponse<DashboardTodoResponse>>> getDashboardPreviews(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        TodoSuccessCode.GET_DASHBOARD,
                        todoFacade.getDashboardPreviews(authUser, date, page, size)
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
    public ResponseEntity<CommonResponse<Void>> updateChildOrder(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long todoId,
            @Valid @RequestBody TodoOrderRequest request
    ) {
        todoFacade.updateChildOrder(authUser, todoId, request);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        TodoSuccessCode.TODO_CHILD_ORDER_UPDATE
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

    // 반복 설정 (이미 반복 중이면 규칙 변경, 반복 중인 회차 어디서든 가능하며 scope 필수 : ALL 전체, FROM_TODAY 오늘부터)
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

    // 반복 해제 (scope 필수 : ALL 전체 해제로 생성된 회차는 일반 투두로 유지, FROM_TODAY 오늘부터 반복 종료)
    @DeleteMapping("/todos/{todoId}/routine")
    public ResponseEntity<CommonResponse<Void>> deleteRoutine(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long todoId,
            @RequestParam RoutineScope scope
    ) {
        todoFacade.deleteRoutine(authUser, todoId, scope);
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
