package com.backend.orbitflow.domain.team.dto;

// 사용자가 가진 팀별 역할 권한 한 줄 (같은 팀에 역할이 여러 개면 여러 줄)
public record TeamMaskRow(Long teamId, int mask) {
}
