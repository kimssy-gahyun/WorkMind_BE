package com.gh.workmind.auth.jwt;

import java.security.Principal;

public record JwtPrincipal(Long memberId, String email, String role) implements Principal {

    public JwtPrincipal {
        if (memberId == null || memberId <= 0 || email == null || email.isBlank()
                || (!"USER".equals(role) && !"ADMIN".equals(role))) {
            throw new IllegalArgumentException("Invalid JWT member claims");
        }
    }

    @Override
    public String getName() {
        return email;
    }
}