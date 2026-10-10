package com.backend.orbitflow.domain.chat.controller;

import com.backend.orbitflow.domain.chat.dto.request.ChatRequests;
import com.backend.orbitflow.domain.chat.dto.response.ChatSuccessCode;
import com.backend.orbitflow.domain.chat.dto.response.ChatroomResponse;
import com.backend.orbitflow.domain.chat.dto.response.MessageResponse;
import com.backend.orbitflow.domain.chat.facade.ChatFacade;
import com.backend.orbitflow.global.common.dto.response.CommonResponse;
import com.backend.orbitflow.global.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.backend.orbitflow.domain.chat.dto.response.ChatroomMemberResponse;
import com.backend.orbitflow.global.common.dto.response.PageResponse;

// 실시간 수신 : STOMP /ws 연결(CONNECT 헤더 Authorization: Bearer {token}) 후 /sub/chatrooms/{uuid} 구독
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ChatController {

    private final ChatFacade chatFacade;

    // ---------- 대화 ----------

    // 1:1 대화 열기 (없으면 생성, 나갔던 대화면 다시 참여)
    @PostMapping("/chatrooms/direct")
    public ResponseEntity<CommonResponse<ChatroomResponse>> openDirect(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody ChatRequests.DirectChatroomRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        ChatSuccessCode.DIRECT_CHATROOM,
                        chatFacade.openDirect(authUser, request)
                ));
    }

    // 다수 참여 대화 생성 (나를 포함해 3명 이상)
    @PostMapping("/chatrooms/group")
    public ResponseEntity<CommonResponse<ChatroomResponse>> createGroup(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody ChatRequests.GroupChatroomRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CommonResponse.success(
                        ChatSuccessCode.CHATROOM_CREATE,
                        chatFacade.createGroup(authUser, request)
                ));
    }

    // 팀 채팅방 생성 (팀당 여러 개, 팀 구성원만 참여)
    @PostMapping("/teams/{teamUuid}/chatrooms")
    public ResponseEntity<CommonResponse<ChatroomResponse>> createTeamGroup(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String teamUuid,
            @Valid @RequestBody ChatRequests.GroupChatroomRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CommonResponse.success(
                        ChatSuccessCode.CHATROOM_CREATE,
                        chatFacade.createTeamGroup(authUser, teamUuid, request)
                ));
    }

    // 마지막 메시지 최신순, 안 읽은 메시지 수 포함 (참여자는 개수만, 목록은 /members)
    @GetMapping("/chatrooms")
    public ResponseEntity<CommonResponse<PageResponse<ChatroomResponse>>> getMyChatrooms(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        ChatSuccessCode.GET_CHATROOM_LIST,
                        chatFacade.getMyChatrooms(authUser, page, size)
                ));
    }

    // 채팅방 참여자 (참여 순)
    @GetMapping("/chatrooms/{chatroomUuid}/members")
    public ResponseEntity<CommonResponse<PageResponse<ChatroomMemberResponse>>> getMembers(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String chatroomUuid,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        ChatSuccessCode.GET_CHATROOM_INFO,
                        chatFacade.getMembers(authUser, chatroomUuid, page, size)
                ));
    }

    @GetMapping("/chatrooms/{chatroomUuid}")
    public ResponseEntity<CommonResponse<ChatroomResponse>> getChatroom(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String chatroomUuid
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        ChatSuccessCode.GET_CHATROOM_INFO,
                        chatFacade.getChatroom(authUser, chatroomUuid)
                ));
    }

    // 1:1 대화에서 초대하면 새 다수 참여 대화가 생성되어 반환됨
    @PostMapping("/chatrooms/{chatroomUuid}/invite")
    public ResponseEntity<CommonResponse<ChatroomResponse>> invite(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String chatroomUuid,
            @Valid @RequestBody ChatRequests.InviteRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        ChatSuccessCode.CHATROOM_INVITE,
                        chatFacade.invite(authUser, chatroomUuid, request)
                ));
    }

    @DeleteMapping("/chatrooms/{chatroomUuid}/members/me")
    public ResponseEntity<CommonResponse<Void>> leave(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String chatroomUuid
    ) {
        chatFacade.leave(authUser, chatroomUuid);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        ChatSuccessCode.CHATROOM_LEAVE
                ));
    }

    @PatchMapping("/chatrooms/{chatroomUuid}/read")
    public ResponseEntity<CommonResponse<Void>> read(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String chatroomUuid
    ) {
        chatFacade.read(authUser, chatroomUuid);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        ChatSuccessCode.CHATROOM_READ
                ));
    }

    // ---------- 메시지 ----------

    // 최신순 페이지, beforeId(선택)를 주면 그보다 오래된 메시지만 : 첫 조회의 최신 메시지 id + 1을 고정해 넘기면 새 메시지가 와도 페이지가 밀리지 않음
    @GetMapping("/chatrooms/{chatroomUuid}/messages")
    public ResponseEntity<CommonResponse<PageResponse<MessageResponse>>> getMessages(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String chatroomUuid,
            @RequestParam(required = false) Long beforeId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "30") int size
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        ChatSuccessCode.GET_MESSAGE_LIST,
                        chatFacade.getMessages(authUser, chatroomUuid, beforeId, page, size)
                ));
    }

    // 응답 수신 = 전송 완료 (응답 전까지 클라이언트는 전송 중 상태 표시)
    @PostMapping("/chatrooms/{chatroomUuid}/messages")
    public ResponseEntity<CommonResponse<MessageResponse>> send(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String chatroomUuid,
            @Valid @RequestBody ChatRequests.SendMessageRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CommonResponse.success(
                        ChatSuccessCode.MESSAGE_SEND,
                        chatFacade.send(authUser, chatroomUuid, request)
                ));
    }

    // scope : ME(내 화면 삭제) 또는 ALL(작성자의 전송 취소)
    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<CommonResponse<Void>> deleteMessage(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long messageId,
            @Valid @RequestBody ChatRequests.DeleteMessageRequest request
    ) {
        chatFacade.deleteMessage(authUser, messageId, request);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        ChatSuccessCode.MESSAGE_DELETE
                ));
    }
}
