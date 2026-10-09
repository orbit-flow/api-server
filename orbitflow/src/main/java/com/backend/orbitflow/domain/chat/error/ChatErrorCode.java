package com.backend.orbitflow.domain.chat.error;

import com.backend.orbitflow.global.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ChatErrorCode implements ErrorCode {

    CHATROOM_NOT_FOUND(HttpStatus.NOT_FOUND,
            "존재하지 않는 대화입니다.",
            "https://orbitflow.com/errors/chatroom-not-found",
            "Chatroom Not Found"),
    SELF_CHAT(HttpStatus.BAD_REQUEST,
            "자기 자신과는 대화할 수 없습니다.",
            "https://orbitflow.com/errors/self-chat",
            "Self Chat"),
    CHAT_BLOCKED(HttpStatus.FORBIDDEN,
            "차단 관계인 사용자와는 대화할 수 없습니다.",
            "https://orbitflow.com/errors/chat-blocked",
            "Chat Blocked"),
    CHAT_INVITE_NOT_ALLOWED(HttpStatus.FORBIDDEN,
            "상대방이 팔로우하지 않는 사용자의 대화 초대를 허용하지 않습니다.",
            "https://orbitflow.com/errors/chat-invite-not-allowed",
            "Chat Invite Not Allowed"),
    GROUP_MIN_MEMBERS(HttpStatus.BAD_REQUEST,
            "다수 참여 대화는 3명 이상이 참여해야 합니다.",
            "https://orbitflow.com/errors/group-min-members",
            "Group Min Members"),
    ALREADY_CHAT_MEMBER(HttpStatus.CONFLICT,
            "이미 대화에 참여 중인 사용자입니다.",
            "https://orbitflow.com/errors/already-chat-member",
            "Already Chat Member"),
    MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND,
            "존재하지 않는 메시지입니다.",
            "https://orbitflow.com/errors/message-not-found",
            "Message Not Found"),
    NOT_MESSAGE_SENDER(HttpStatus.FORBIDDEN,
            "메시지 작성자만 전송을 취소할 수 있습니다.",
            "https://orbitflow.com/errors/not-message-sender",
            "Not Message Sender"),
    ALREADY_UNSENT(HttpStatus.CONFLICT,
            "이미 전송이 취소된 메시지입니다.",
            "https://orbitflow.com/errors/already-unsent",
            "Already Unsent");

    private final HttpStatus httpStatus;
    private final String message;
    private final String type;
    private final String title;
}
