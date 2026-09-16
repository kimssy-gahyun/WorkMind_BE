package com.gh.workmind.member.model.dto;

import java.time.LocalDateTime;

public record MemberProfileResponse(
        Long memberId,
        String email,
        String name,
        String role,
        LocalDateTime enrollDate,
        LocalDateTime modifyDate) {
}