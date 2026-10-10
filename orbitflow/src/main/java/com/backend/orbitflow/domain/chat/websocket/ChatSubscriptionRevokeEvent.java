package com.backend.orbitflow.domain.chat.websocket;

// 채팅방 참여 권한 변경에 따른 STOMP 구독 해제 요청 (커밋 이후 ChatSubscriptionRevokeListener가 처리)
// recheck=false : userUuid의 chatroomUuid 구독 해제 (chatroomUuid가 null이면 사용자의 모든 구독)
// recheck=true  : 현재 구독 중인 채팅방 중 참여자가 아닌 것만 해제 (userUuid가 null이면 모든 사용자 대상)
public record ChatSubscriptionRevokeEvent(String userUuid, String chatroomUuid, boolean recheck) {

    public static ChatSubscriptionRevokeEvent chatroom(String userUuid, String chatroomUuid) {
        return new ChatSubscriptionRevokeEvent(userUuid, chatroomUuid, false);
    }

    public static ChatSubscriptionRevokeEvent recheck(String userUuid) {
        return new ChatSubscriptionRevokeEvent(userUuid, null, true);
    }

    public static ChatSubscriptionRevokeEvent recheckAll() {
        return new ChatSubscriptionRevokeEvent(null, null, true);
    }
}
