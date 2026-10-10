package com.backend.orbitflow.domain.todo.service;

import com.backend.orbitflow.domain.category.entity.Category;
import com.backend.orbitflow.domain.team.enums.TeamPermission;
import com.backend.orbitflow.domain.team.error.TeamErrorCode;
import com.backend.orbitflow.domain.todo.entity.Todo;
import com.backend.orbitflow.domain.todo.error.TodoErrorCode;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.global.common.error.exception.CommonException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 투두는 소속 카테고리의 공개 범위를 따름
// 개인 투두 : 카테고리 소유자만 생성·수정·삭제, 팀 투두 : MANAGE_TODOS 권한 보유자만 생성·수정·삭제
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodoAuthorityServiceImpl implements TodoAuthorityService {

    public void checkView(boolean viewable) {
        if (!viewable) {
            throw new CommonException(TodoErrorCode.TODO_ACCESS_DENIED);
        }
    }

    public void checkEdit(Category category, User actor, Integer teamMask) {
        if (category.isTeamCategory()) {
            checkPermission(teamMask, TeamPermission.MANAGE_TODOS);
            return;
        }
        if (!category.isOwnedBy(actor)) {
            throw new CommonException(TodoErrorCode.TODO_EDIT_DENIED);
        }
    }

    // 팀 투두는 담당자 본인 또는 MANAGE_TODOS 권한 보유자가 완료 처리
    public void checkComplete(Todo todo, User actor, Integer teamMask) {
        Category category = todo.getCategory();
        if (category.isTeamCategory() && todo.getAssignee().getId().equals(actor.getId())) {
            requireMember(teamMask);
            return;
        }
        checkEdit(category, actor, teamMask);
    }

    // ASSIGN_TODOS 권한 보유자만 배정, 담당자는 팀 구성원이어야 함
    public void checkAssign(Integer actorMask, boolean assigneeMember) {
        checkPermission(actorMask, TeamPermission.ASSIGN_TODOS);
        if (!assigneeMember) {
            throw new CommonException(TeamErrorCode.MEMBER_NOT_FOUND);
        }
    }

    public void checkAssignOnCreate(Category category, User actor, User assignee, Integer actorMask, boolean assigneeMember) {
        if (category.isTeamCategory() && !assignee.getId().equals(actor.getId())) {
            checkAssign(actorMask, assigneeMember);
        }
    }

    // 비소속 사용자에게는 팀 존재 여부를 노출하지 않도록 NOT_FOUND 처리 (TeamAuthorityService.checkPermission과 같은 규칙)
    private void checkPermission(Integer teamMask, TeamPermission permission) {
        requireMember(teamMask);
        if (!permission.isGranted(teamMask)) {
            throw new CommonException(TeamErrorCode.NO_TEAM_PERMISSION);
        }
    }

    private void requireMember(Integer teamMask) {
        if (teamMask == null) {
            throw new CommonException(TeamErrorCode.TEAM_NOT_FOUND);
        }
    }
}
