package com.backend.orbitflow.domain.chat.dto.request;

import com.backend.orbitflow.domain.chat.enums.MessageDeleteScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class ChatRequests {

    private ChatRequests() {
    }

    public record DirectChatroomRequest(
            @NotBlank(message = "대화 상대 uuid는 비어있을 수 없습니다.")
            String userUuid
    ) {
    }

    // 나를 제외한 참여자 2명 이상
    public record GroupChatroomRequest(
            @Size(max = 100, message = "대화 이름은 100자 이내여야 합니다.")
            String title,

            @NotEmpty(message = "참여자는 비어있을 수 없습니다.")
            @Size(max = 50, message = "한 번에 최대 50명까지 참여할 수 있습니다.")
            List<String> userUuids
    ) {
    }

    public record InviteRequest(
            @NotEmpty(message = "초대할 사용자는 비어있을 수 없습니다.")
            @Size(max = 50, message = "한 번에 최대 50명까지 초대할 수 있습니다.")
            List<String> userUuids
    ) {
    }

    public record SendMessageRequest(
            @NotBlank(message = "메시지 내용은 비어있을 수 없습니다.")
            @Size(max = 2000, message = "메시지는 2000자 이내여야 합니다.")
            String content,

            // 클라이언트가 생성한 전송 요청 id : 전송 중 상태 표시와 재시도 중복 방지용 (선택)
            @Size(max = 64, message = "clientMessageId는 64자 이내여야 합니다.")
            String clientMessageId
    ) {
    }

    public record DeleteMessageRequest(
            @NotNull(message = "삭제 방식은 비어있을 수 없습니다.")
            MessageDeleteScope scope
    ) {
    }
}
