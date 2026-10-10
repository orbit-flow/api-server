package com.backend.orbitflow.domain.todo.service;

import com.backend.orbitflow.domain.category.entity.Category;
import com.backend.orbitflow.domain.todo.entity.Todo;
import com.backend.orbitflow.domain.user.entity.User;

// 다른 도메인 서비스를 호출하지 않는 순수 정책 : 판정에 필요한 열람 가능 여부·팀 권한은 호출 측(Facade)이 조회해서 전달
// teamMask : 팀 구성원의 권한 mask, 구성원이 아니면 null (개인 카테고리에서는 사용하지 않음)
public interface TodoAuthorityService {

    // viewable : 소속 카테고리의 열람 가능 여부 (투두는 카테고리의 공개 범위를 따름)
    void checkView(boolean viewable);
    void checkEdit(Category category, User actor, Integer teamMask);
    void checkComplete(Todo todo, User actor, Integer teamMask);
    // actorMask : 배정하는 사용자의 권한 mask, assigneeMember : 담당자가 팀 구성원인지
    void checkAssign(Integer actorMask, boolean assigneeMember);
    // 투두 생성 시 배정 : 팀 투두를 다른 구성원에게 지정하는 경우에만 검사
    void checkAssignOnCreate(Category category, User actor, User assignee, Integer actorMask, boolean assigneeMember);
}
