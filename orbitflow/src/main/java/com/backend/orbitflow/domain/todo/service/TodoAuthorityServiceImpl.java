package com.backend.orbitflow.domain.todo.service;

import com.backend.orbitflow.domain.category.entity.Category;
import com.backend.orbitflow.domain.category.service.CategoryAuthorityService;
import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.team.entity.TeamMember;
import com.backend.orbitflow.domain.team.enums.TeamPermission;
import com.backend.orbitflow.domain.team.error.TeamErrorCode;
import com.backend.orbitflow.domain.team.service.TeamAuthorityService;
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

    private final CategoryAuthorityService categoryAuthorityService;
    private final TeamAuthorityService teamAuthorityService;

    public void checkView(Category category, User viewer) {
        if (!categoryAuthorityService.canView(category, viewer)) {
            throw new CommonException(TodoErrorCode.TODO_ACCESS_DENIED);
        }
    }

    public void checkEdit(Category category, User actor) {
        if (category.isTeamCategory()) {
            teamAuthorityService.checkPermission(category.getTeam(), actor, TeamPermission.MANAGE_TODOS);
            return;
        }
        if (!category.isOwnedBy(actor)) {
            throw new CommonException(TodoErrorCode.TODO_EDIT_DENIED);
        }
    }

    // 팀 투두는 담당자 본인 또는 MANAGE_TODOS 권한 보유자가 완료 처리
    public void checkComplete(Todo todo, User actor) {
        Category category = todo.getCategory();
        if (category.isTeamCategory() && todo.getAssignee().getId().equals(actor.getId())) {
            teamAuthorityService.getMember(category.getTeam(), actor);
            return;
        }
        checkEdit(category, actor);
    }

    // ASSIGN_TODOS 권한 보유자만 배정, 담당자는 팀 구성원이어야 함
    public void checkAssign(Category category, User actor, User assignee) {
        Team team = category.getTeam();
        teamAuthorityService.checkPermission(team, actor, TeamPermission.ASSIGN_TODOS);
        TeamMember member = teamAuthorityService.findMember(team, assignee).orElse(null);
        if (member == null) {
            throw new CommonException(TeamErrorCode.MEMBER_NOT_FOUND);
        }
    }
}
