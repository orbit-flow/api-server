package com.backend.orbitflow.domain.chat.dto.response;

import com.backend.orbitflow.global.common.dto.response.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ChatSuccessCode implements SuccessCode {

    CHATROOM_CREATE(HttpStatus.CREATED, "대화가 생성되었습니다."),
    DIRECT_CHATROOM(HttpStatus.OK, "1:1 대화가 열람되었습니다."),
    GET_CHATROOM_LIST(HttpStatus.OK, "대화 리스트가 열람되었습니다."),
    GET_CHATROOM_INFO(HttpStatus.OK, "대화 정보가 열람되었습니다."),
    CHATROOM_INVITE(HttpStatus.OK, "대화에 초대했습니다."),
    CHATROOM_LEAVE(HttpStatus.OK, "대화에서 나갔습니다."),
    CHATROOM_READ(HttpStatus.OK, "대화를 읽음 처리했습니다."),
    GET_MESSAGE_LIST(HttpStatus.OK, "메시지 리스트가 열람되었습니다."),
    MESSAGE_SEND(HttpStatus.CREATED, "메시지를 전송했습니다."),
    MESSAGE_DELETE(HttpStatus.OK, "메시지가 삭제되었습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
