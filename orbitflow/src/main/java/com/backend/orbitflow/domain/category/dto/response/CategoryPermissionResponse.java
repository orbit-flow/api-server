package com.backend.orbitflow.domain.category.dto.response;

import com.backend.orbitflow.domain.category.enums.CategoryPermissionTargetType;

// PRIVATE 팀 카테고리의 열람 허용 대상 한 줄 (평면 구조, 목록은 Page<CategoryPermissionResponse>)
// type이 ROLE이면 role* 필드, MEMBER면 member* 필드만 값이 있음 (memberUuid : 사용자 uuid)
public record CategoryPermissionResponse(
        CategoryPermissionTargetType type,
        Long roleId,
        String roleName,
        String roleColor,
        String memberUuid,
        String memberName,
        String memberNickname
) {

    // CategoryPermissionRepository JPQL 생성자 표현식에서 사용 (역할 id 유무로 종류 판정)
    public CategoryPermissionResponse(Long roleId, String roleName, String roleColor,
                                      String memberUuid, String memberName, String memberNickname) {
        this(roleId != null ? CategoryPermissionTargetType.ROLE : CategoryPermissionTargetType.MEMBER,
                roleId, roleName, roleColor, memberUuid, memberName, memberNickname);
    }
}
