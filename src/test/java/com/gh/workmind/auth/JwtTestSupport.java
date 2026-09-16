package com.gh.workmind.auth;

import java.util.Base64;

import org.springframework.test.context.DynamicPropertyRegistry;

import io.jsonwebtoken.Jwts;

public final class JwtTestSupport {

    public static final String SECRET =
            Base64.getEncoder().encodeToString(Jwts.SIG.HS256.key().build().getEncoded());

    private JwtTestSupport() {
    }

    public static void configure(DynamicPropertyRegistry registry) {
        registry.add("jwt.secret", () -> SECRET);
        registry.add("jwt.access-token-expiration-seconds", () -> 1800);
    }
}