package com.backend.orbitflow.domain.team.error;

import com.backend.orbitflow.global.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TeamErrorCode implements ErrorCode {

    TEAM_NOT_FOUND(HttpStatus.NOT_FOUND,
            "존재하지 않는 팀입니다.",
            "https://orbitflow.com/errors/team-not-found",
            "Team Not Found"),
    NOT_TEAM_OWNER(HttpStatus.FORBIDDEN,
            "팀 소유자만 수행할 수 있습니다.",
            "https://orbitflow.com/errors/not-team-owner",
            "Not Team Owner"),
    NO_TEAM_PERMISSION(HttpStatus.FORBIDDEN,
            "해당 팀 작업을 수행할 권한이 없습니다.",
            "https://orbitflow.com/errors/no-team-permission",
            "No Team Permission"),
    CANNOT_GRANT_PERMISSION(HttpStatus.FORBIDDEN,
            "보유하지 않은 권한은 부여하거나 회수할 수 없습니다.",
            "https://orbitflow.com/errors/cannot-grant-permission",
            "Cannot Grant Permission"),

    // 구성원
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND,
            "팀 구성원이 아닙니다.",
            "https://orbitflow.com/errors/member-not-found",
            "Member Not Found"),
    ALREADY_TEAM_MEMBER(HttpStatus.CONFLICT,
            "이미 팀 구성원입니다.",
            "https://orbitflow.com/errors/already-team-member",
            "Already Team Member"),
    OWNER_CANNOT_LEAVE(HttpStatus.BAD_REQUEST,
            "팀 소유자는 소유자를 다른 구성원에게 넘긴 뒤에만 탈퇴할 수 있습니다.",
            "https://orbitflow.com/errors/owner-cannot-leave",
            "Owner Cannot Leave"),
    CANNOT_KICK_OWNER(HttpStatus.BAD_REQUEST,
            "팀 소유자는 추방할 수 없습니다.",
            "https://orbitflow.com/errors/cannot-kick-owner",
            "Cannot Kick Owner"),
    SELF_KICK(HttpStatus.BAD_REQUEST,
            "자기 자신은 추방할 수 없습니다. 팀 탈퇴를 이용해주세요.",
            "https://orbitflow.com/errors/self-kick",
            "Self Kick"),
    ALREADY_TEAM_OWNER(HttpStatus.BAD_REQUEST,
            "이미 팀 소유자입니다.",
            "https://orbitflow.com/errors/already-team-owner",
            "Already Team Owner"),

    // 역할
    ROLE_NOT_FOUND(HttpStatus.NOT_FOUND,
            "존재하지 않는 역할입니다.",
            "https://orbitflow.com/errors/role-not-found",
            "Role Not Found"),
    DEFAULT_ROLE_DELETE(HttpStatus.BAD_REQUEST,
            "기본 역할은 삭제할 수 없습니다. 다른 역할을 기본 역할로 지정한 뒤 삭제해주세요.",
            "https://orbitflow.com/errors/default-role-delete",
            "Default Role Delete"),

    // 초대
    INVITATION_NOT_FOUND(HttpStatus.NOT_FOUND,
            "존재하지 않는 초대입니다.",
            "https://orbitflow.com/errors/invitation-not-found",
            "Invitation Not Found"),
    INVITATION_NOT_PENDING(HttpStatus.CONFLICT,
            "이미 처리된 초대입니다.",
            "https://orbitflow.com/errors/invitation-not-pending",
            "Invitation Not Pending"),
    ALREADY_INVITED(HttpStatus.CONFLICT,
            "이미 대기 중인 초대가 있습니다.",
            "https://orbitflow.com/errors/already-invited",
            "Already Invited"),
    SELF_INVITE(HttpStatus.BAD_REQUEST,
            "자기 자신은 초대할 수 없습니다.",
            "https://orbitflow.com/errors/self-invite",
            "Self Invite");

    private final HttpStatus httpStatus;
    private final String message;
    private final String type;
    private final String title;
}
