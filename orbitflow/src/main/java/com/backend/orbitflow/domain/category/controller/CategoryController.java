package com.backend.orbitflow.domain.category.controller;

import com.backend.orbitflow.domain.category.dto.request.CategoryPermissionRequest;
import com.backend.orbitflow.domain.category.dto.request.CategoryRequest;
import com.backend.orbitflow.domain.category.dto.response.CategoryPermissionResponse;
import com.backend.orbitflow.domain.category.dto.response.CategoryResponse;
import com.backend.orbitflow.domain.category.dto.response.CategorySuccessCode;
import com.backend.orbitflow.domain.category.facade.CategoryFacade;
import com.backend.orbitflow.global.common.dto.response.CommonResponse;
import com.backend.orbitflow.global.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CategoryController {

    private final CategoryFacade categoryFacade;

    // ---------- 개인 카테고리 ----------

    @PostMapping("/categories")
    public ResponseEntity<CommonResponse<CategoryResponse>> createPersonalCategory(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody CategoryRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CommonResponse.success(
                        CategorySuccessCode.CATEGORY_CREATE,
                        categoryFacade.createPersonalCategory(authUser, request)
                ));
    }

    @GetMapping("/categories/me")
    public ResponseEntity<CommonResponse<List<CategoryResponse>>> getMyCategories(
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        CategorySuccessCode.GET_CATEGORY_LIST,
                        categoryFacade.getMyCategories(authUser)
                ));
    }

    // 다른 사용자의 카테고리 중 내가 열람 가능한 것만
    @GetMapping("/users/{userUuid}/categories")
    public ResponseEntity<CommonResponse<List<CategoryResponse>>> getUserCategories(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String userUuid
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        CategorySuccessCode.GET_CATEGORY_LIST,
                        categoryFacade.getUserCategories(authUser, userUuid)
                ));
    }

    // ---------- 팀 카테고리 ----------

    @PostMapping("/teams/{teamUuid}/categories")
    public ResponseEntity<CommonResponse<CategoryResponse>> createTeamCategory(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String teamUuid,
            @Valid @RequestBody CategoryRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CommonResponse.success(
                        CategorySuccessCode.CATEGORY_CREATE,
                        categoryFacade.createTeamCategory(authUser, teamUuid, request)
                ));
    }

    // 팀 카테고리 중 내가 열람 가능한 것만
    @GetMapping("/teams/{teamUuid}/categories")
    public ResponseEntity<CommonResponse<List<CategoryResponse>>> getTeamCategories(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String teamUuid
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        CategorySuccessCode.GET_CATEGORY_LIST,
                        categoryFacade.getTeamCategories(authUser, teamUuid)
                ));
    }

    // ---------- 공통 ----------

    @GetMapping("/categories/{categoryId}")
    public ResponseEntity<CommonResponse<CategoryResponse>> getCategory(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long categoryId
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        CategorySuccessCode.GET_CATEGORY_INFO,
                        categoryFacade.getCategory(authUser, categoryId)
                ));
    }

    @PutMapping("/categories/{categoryId}")
    public ResponseEntity<CommonResponse<CategoryResponse>> updateCategory(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long categoryId,
            @Valid @RequestBody CategoryRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        CategorySuccessCode.CATEGORY_UPDATE,
                        categoryFacade.updateCategory(authUser, categoryId, request)
                ));
    }

    // 소속 투두를 옮길 같은 소유자의 다른 카테고리 지정 필수
    @DeleteMapping("/categories/{categoryId}")
    public ResponseEntity<CommonResponse<Void>> deleteCategory(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long categoryId,
            @RequestParam Long moveToCategoryId
    ) {
        categoryFacade.deleteCategory(authUser, categoryId, moveToCategoryId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        CategorySuccessCode.CATEGORY_DELETE
                ));
    }

    // PRIVATE 팀 카테고리의 열람 허용 역할·팀원
    @GetMapping("/categories/{categoryId}/permissions")
    public ResponseEntity<CommonResponse<CategoryPermissionResponse>> getPermissions(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long categoryId
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        CategorySuccessCode.GET_CATEGORY_PERMISSION,
                        categoryFacade.getPermissions(authUser, categoryId)
                ));
    }

    @PutMapping("/categories/{categoryId}/permissions")
    public ResponseEntity<CommonResponse<CategoryPermissionResponse>> updatePermissions(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long categoryId,
            @Valid @RequestBody CategoryPermissionRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        CategorySuccessCode.CATEGORY_PERMISSION_UPDATE,
                        categoryFacade.updatePermissions(authUser, categoryId, request)
                ));
    }
}
