package com.backend.orbitflow.domain.team.dto.response;

import com.backend.orbitflow.global.common.dto.response.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TeamSuccessCode implements SuccessCode {

    TEAM_CREATE(HttpStatus.CREATED, "팀이 생성되었습니다."),
    GET_TEAM_LIST(HttpStatus.OK, "팀 리스트가 열람되었습니다."),
    GET_TEAM_INFO(HttpStatus.OK, "팀 정보가 열람되었습니다."),
    TEAM_UPDATE(HttpStatus.OK, "팀 정보가 업데이트 되었습니다."),
    TEAM_DELETE(HttpStatus.OK, "팀이 삭제되었습니다."),
    GET_DELETED_TEAM_LIST(HttpStatus.OK, "복구 가능한 팀 리스트가 열람되었습니다."),
    TEAM_RESTORE(HttpStatus.OK, "팀이 복구되었습니다."),

    // 구성원
    GET_MEMBER_LIST(HttpStatus.OK, "팀 구성원 리스트가 열람되었습니다."),
    GET_MY_MEMBER_INFO(HttpStatus.OK, "내 팀 프로필이 열람되었습니다."),
    MEMBER_PROFILE_UPDATE(HttpStatus.OK, "팀 프로필이 업데이트 되었습니다."),
    MEMBER_LEAVE(HttpStatus.OK, "팀에서 탈퇴했습니다."),
    MEMBER_KICK(HttpStatus.OK, "구성원이 추방되었습니다."),
    MEMBER_ROLE_UPDATE(HttpStatus.OK, "구성원의 역할이 변경되었습니다."),
    OWNER_TRANSFER(HttpStatus.OK, "팀 소유자가 변경되었습니다."),

    // 역할
    GET_ROLE_LIST(HttpStatus.OK, "역할 리스트가 열람되었습니다."),
    ROLE_CREATE(HttpStatus.CREATED, "역할이 생성되었습니다."),
    ROLE_UPDATE(HttpStatus.OK, "역할이 업데이트 되었습니다."),
    ROLE_DELETE(HttpStatus.OK, "역할이 삭제되었습니다."),
    DEFAULT_ROLE_UPDATE(HttpStatus.OK, "기본 역할이 변경되었습니다."),

    // 초대
    INVITATION_CREATE(HttpStatus.CREATED, "팀 초대를 보냈습니다."),
    GET_INVITATION_LIST(HttpStatus.OK, "초대 리스트가 열람되었습니다."),
    INVITATION_CANCEL(HttpStatus.OK, "초대가 취소되었습니다."),
    INVITATION_ACCEPT(HttpStatus.OK, "초대를 수락했습니다."),
    INVITATION_REJECT(HttpStatus.OK, "초대를 거절했습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
