package com.backend.orbitflow.domain.block.controller;

import com.backend.orbitflow.domain.block.dto.response.BlockListResponse;
import com.backend.orbitflow.domain.block.dto.response.BlockResponse;
import com.backend.orbitflow.domain.block.dto.response.BlockSuccessCode;
import com.backend.orbitflow.domain.block.facade.BlockFacade;
import com.backend.orbitflow.global.common.dto.response.CommonResponse;
import com.backend.orbitflow.global.common.dto.response.PageResponse;
import com.backend.orbitflow.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/blocks")
public class BlockController {

    private final BlockFacade blockFacade;

    // 내가 차단한 사용자 목록
    @GetMapping
    public ResponseEntity<CommonResponse<PageResponse<BlockListResponse>>> getBlocks(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        BlockSuccessCode.GET_BLOCK_LIST,
                        blockFacade.getBlockList(authUser, page, size, keyword)
                ));
    }

    // 차단 / 차단 해제 토글
    @PostMapping("/{uuid}")
    public ResponseEntity<CommonResponse<BlockResponse>> toggleBlock(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String uuid
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        BlockSuccessCode.BLOCK_SUCCESS,
                        blockFacade.toggleBlock(authUser, uuid)
                ));
    }
}
