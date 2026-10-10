package com.backend.orbitflow.domain.category.service;

import com.backend.orbitflow.domain.category.dto.response.CategoryPermissionResponse;
import com.backend.orbitflow.domain.category.dto.response.CategoryResponse;
import com.backend.orbitflow.domain.category.entity.Category;
import com.backend.orbitflow.domain.category.enums.Visibility;
import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.team.entity.TeamMember;
import com.backend.orbitflow.domain.team.entity.TeamRole;
import com.backend.orbitflow.domain.user.entity.User;

import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;

// 권한 검사는 호출 측(Facade)에서 CategoryAuthorityService로 수행
public interface CategoryService {

    Category getActiveCategory(Long categoryId);
    CategoryResponse createPersonalCategory(User user, String name, String color, Visibility visibility);
    CategoryResponse createTeamCategory(Team team, String name, String color, Visibility visibility);
    Page<CategoryResponse> getMyCategories(User me, int page, int size);
    Page<CategoryResponse> getUserCategories(User viewer, User owner, int page, int size);
    Page<CategoryResponse> getTeamCategories(User viewer, Team team, int page, int size);
    CategoryResponse getCategory(Category category);
    CategoryResponse updateCategory(Category category, String name, String color, Visibility visibility);
    Category getMoveTarget(Category category, Long moveToCategoryId);
    void deleteCategory(Category category);
    void deleteAllMemberPermissionsByTeam(Team team);
    void requireTeamCategory(Category category);
    Page<CategoryPermissionResponse> getPermissions(Category category, int page, int size);
    void updatePermissions(Category category, Set<Long> roleIds, List<TeamRole> roles, List<User> members, List<TeamMember> teamMembers);
}
