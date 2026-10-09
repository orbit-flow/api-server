package com.backend.orbitflow.domain.team.dto;

// 사용자의 팀 소속 한 줄 (소유자 판정용 ownerId 포함)
public record TeamMembershipRow(Long teamId, Long ownerId) {
}
