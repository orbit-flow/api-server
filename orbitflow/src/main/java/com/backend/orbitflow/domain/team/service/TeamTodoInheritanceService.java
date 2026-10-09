package com.backend.orbitflow.domain.team.service;

import com.backend.orbitflow.domain.category.entity.Category;
import com.backend.orbitflow.domain.category.service.CategoryAuthorityService;
import com.backend.orbitflow.domain.notification.event.TodosInheritedEvent;
import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.team.entity.TeamMember;
import com.backend.orbitflow.domain.team.repository.TeamMemberRepository;
import com.backend.orbitflow.domain.todo.repository.TodoRepository;
import com.backend.orbitflow.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 구성원이 팀을 떠날 때(탈퇴·추방·회원 탈퇴 후 영구 삭제) 담당하던 미완료 팀 투두 상속
 *
 * <ul>
 *   <li>투두가 속한 카테고리를 볼 수 있는 남은 구성원 중 권한이 가장 낮은 구성원이 상속</li>
 *   <li>권한 비교 : 보유 권한 수(비트 수)가 적을수록 낮음 → 같으면 mask 값이 작을수록 낮음 → 같으면 먼저 가입한 구성원</li>
 *   <li>소유자는 항상 전체 권한이므로 다른 후보가 없을 때만 상속 (소유자는 모든 카테고리를 볼 수 있어 상속 대상이 항상 존재)</li>
 *   <li>상속받은 구성원에게는 팀별로 묶어 알림 1건 발송</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class TeamTodoInheritanceService {

    private final TodoRepository todoRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamAuthorityService teamAuthorityService;
    private final CategoryAuthorityService categoryAuthorityService;
    private final ApplicationEventPublisher eventPublisher;

    private record Candidate(TeamMember member, int mask) {
    }

    private static final Comparator<Candidate> LOWEST_PERMISSION_FIRST = Comparator
            .comparingInt((Candidate c) -> Integer.bitCount(c.mask()))
            .thenComparingInt(Candidate::mask)
            .thenComparing(c -> c.member().getCreatedAt())
            .thenComparing(c -> c.member().getId());

    public void inherit(Team team, User leaver) {
        List<Category> categories = todoRepository.findCategoriesWithIncompleteTodos(team, leaver);
        if (categories.isEmpty()) {
            return;
        }
        List<Candidate> candidates = teamMemberRepository.findAllWithUserByTeam(team).stream()
                .filter(member -> !member.getUser().getId().equals(leaver.getId()))
                .map(member -> new Candidate(member, teamAuthorityService.getPermissionMask(team, member)))
                .sorted(LOWEST_PERMISSION_FIRST)
                .toList();

        // 대상 결정을 모두 마친 뒤 일괄 변경 (일괄 변경이 영속성 컨텍스트를 비우므로)
        Map<Category, User> heirs = new LinkedHashMap<>();
        for (Category category : categories) {
            findHeir(category, candidates).ifPresent(heir -> heirs.put(category, heir));
        }

        Map<Long, Integer> inheritedCounts = new LinkedHashMap<>();
        heirs.forEach((category, heir) -> {
            int count = todoRepository.reassignIncompleteTodos(category, leaver, heir);
            inheritedCounts.merge(heir.getId(), count, Integer::sum);
        });
        inheritedCounts.forEach((heirId, count) -> {
            if (count > 0) {
                eventPublisher.publishEvent(new TodosInheritedEvent(team.getId(), leaver.getId(), heirId, count));
            }
        });
    }

    private Optional<User> findHeir(Category category, List<Candidate> candidates) {
        return candidates.stream()
                .map(candidate -> candidate.member().getUser())
                .filter(user -> categoryAuthorityService.canView(category, user))
                .findFirst();
    }
}
