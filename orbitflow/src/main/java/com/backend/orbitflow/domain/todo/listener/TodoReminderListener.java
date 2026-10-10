package com.backend.orbitflow.domain.todo.listener;

import com.backend.orbitflow.domain.todo.reminder.TodoReminderQueue;
import com.backend.orbitflow.domain.todo.entity.Todo;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// 투두가 저장·수정·삭제될 때 리마인드 예약 목록(Redis) 갱신
// 생성·수정·완료·논리 삭제·복구·반복 회차 생성 등 모든 저장 경로를 한 곳에서 처리 (호출부 누락 방지)
// JPQL 일괄 변경 중 카테고리 이동·담당자 상속은 리마인드 시각에 영향이 없고, 영구 삭제는 발송 직전 확인에서 걸러짐
// 부모 복구 시 함께 복구되는 자식 투두는 엔티티 리스너를 거치지 않으므로 TodoServiceImpl.restoreTodo에서 직접 예약
@Component
@RequiredArgsConstructor
public class TodoReminderListener {

    private final TodoReminderQueue todoReminderQueue;

    @PostPersist
    @PostUpdate
    public void onSave(Todo todo) {
        todoReminderQueue.scheduleAfterCommit(todo.getId(), todo.getRemindAt());
    }

    @PostRemove
    public void onRemove(Todo todo) {
        todoReminderQueue.scheduleAfterCommit(todo.getId(), null);
    }
}
