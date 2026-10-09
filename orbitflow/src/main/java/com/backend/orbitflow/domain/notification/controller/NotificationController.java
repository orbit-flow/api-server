package com.backend.orbitflow.domain.notification.controller;

import com.backend.orbitflow.domain.notification.dto.response.NotificationResponse;
import com.backend.orbitflow.domain.notification.dto.response.NotificationSuccessCode;
import com.backend.orbitflow.domain.notification.facade.NotificationFacade;
import com.backend.orbitflow.global.common.dto.response.CommonResponse;
import com.backend.orbitflow.global.common.dto.response.PageResponse;
import com.backend.orbitflow.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationFacade notificationFacade;

    // 실시간 알림 구독 (SSE, event name : notification)
    // Authorization 헤더가 필요하므로 FE는 헤더를 지원하는 EventSource 구현(event-source-polyfill 등) 사용
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return notificationFacade.subscribe(authUser);
    }

    // 만료되지 않은(생성 후 30일 이내) 알림, unreadOnly=true면 안 읽은 알림만
    @GetMapping
    public ResponseEntity<CommonResponse<PageResponse<NotificationResponse>>> getNotifications(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        NotificationSuccessCode.GET_NOTIFICATION_LIST,
                        notificationFacade.getNotifications(authUser, unreadOnly, page, size)
                ));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<CommonResponse<Long>> countUnread(
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        NotificationSuccessCode.GET_UNREAD_COUNT,
                        notificationFacade.countUnread(authUser)
                ));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<CommonResponse<NotificationResponse>> read(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long notificationId
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        NotificationSuccessCode.NOTIFICATION_READ,
                        notificationFacade.read(authUser, notificationId)
                ));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<CommonResponse<Void>> readAll(
            @AuthenticationPrincipal AuthUser authUser
    ) {
        notificationFacade.readAll(authUser);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        NotificationSuccessCode.NOTIFICATION_READ_ALL
                ));
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<CommonResponse<Void>> delete(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long notificationId
    ) {
        notificationFacade.delete(authUser, notificationId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        NotificationSuccessCode.NOTIFICATION_DELETE
                ));
    }
}
