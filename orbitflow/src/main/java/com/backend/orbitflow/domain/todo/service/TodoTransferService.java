package com.backend.orbitflow.domain.todo.service;

import com.backend.orbitflow.domain.category.entity.Category;
import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.todo.repository.TodoRepository;
import com.backend.orbitflow.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 투두 일괄 이관 (카테고리에 의존하지 않으므로 카테고리·팀 도메인에서 호출 가능)
 *
 * <p>구성원이 팀을 떠날 때(탈퇴·추방·회원 탈퇴 후 영구 삭제) 담당하던 미완료 팀 투두 상속</p>
 * <ul>
 *   <li>투두가 속한 카테고리를 볼 수 있는 남은 구성원 중 권한이 가장 낮은 구성원이 상속 (판정은 TodoRepository.findHeirs 쿼리)</li>
 *   <li>권한 비교 : 보유 권한 수(비트 수)가 적을수록 낮음 → 같으면 mask 값이 작을수록 낮음 → 같으면 먼저 가입한 구성원</li>
 *   <li>소유자는 항상 전체 권한이므로 다른 후보가 없을 때만 상속 (소유자는 모든 카테고리를 볼 수 있어 상속 대상이 항상 존재)</li>
 *   <li>쿼리 : 대상 카테고리 1 + 상속 대상 1 + 받는 사람마다 변경 1</li>
 *   <li>상속받은 구성원에게는 호출 측이 팀별로 묶어 알림 1건 발송</li>
 * </ul>
 * <p>카테고리 삭제 시 모든 소속 투두(논리적 삭제 포함)를 같은 소유자의 다른 카테고리로 이동</p>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class TodoTransferService {

    private final TodoRepository todoRepository;

    // 상속받은 구성원 id → 상속받은 투두 수 (알림은 호출 측에서 발송)
    public Map<Long, Integer> inherit(Team team, User leaver) {
        List<Long> categoryIds = todoRepository.findCategoriesWithIncompleteTodos(team, leaver).stream()
                .map(Category::getId)
                .toList();
        if (categoryIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<Long>> categoryIdsByHeir = new LinkedHashMap<>();
        for (TodoRepository.Heir heir : todoRepository.findHeirs(team.getId(), leaver.getId(), categoryIds)) {
            categoryIdsByHeir.computeIfAbsent(heir.getHeirId(), key -> new ArrayList<>()).add(heir.getCategoryId());
        }
        Map<Long, Integer> countByHeir = new LinkedHashMap<>();
        categoryIdsByHeir.forEach((heirId, heirCategoryIds) -> {
            int count = todoRepository.reassignIncompleteTodos(heirCategoryIds, leaver.getId(), heirId);
            if (count > 0) {
                countByHeir.put(heirId, count);
            }
        });
        return countByHeir;
    }

    public void moveAllToCategory(Category from, Category to) {
        todoRepository.moveAllToCategory(from, to);
    }
}
