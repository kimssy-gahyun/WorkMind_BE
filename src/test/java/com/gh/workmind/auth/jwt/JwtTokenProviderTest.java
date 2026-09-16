package com.gh.workmind.auth.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;

import org.junit.jupiter.api.Test;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;

class JwtTokenProviderTest {

    private final String secret = Base64.getEncoder().encodeToString(Jwts.SIG.HS256.key().build().getEncoded());
    private final Instant now = Instant.parse("2026-09-16T00:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private final JwtTokenProvider provider = new JwtTokenProvider(secret, 1800, clock);

    @Test
    void createsSignedTokenWithMemberClaimsAndConfiguredExpiration() {
        Claims claims = provider.parseAndValidate(provider.createAccessToken(42L, "member@example.com", "USER"));

        assertEquals("42", claims.getSubject());
        assertEquals(42L, claims.get("memberId", Long.class));
        assertEquals("member@example.com", claims.get("email", String.class));
        assertEquals("USER", claims.get("role", String.class));
        assertEquals(Date.from(now), claims.getIssuedAt());
        assertEquals(Date.from(now.plusSeconds(1800)), claims.getExpiration());
    }

    @Test
    void rejectsExpiredTokenWithoutWaiting() {
        String token = provider.createAccessToken(42L, "member@example.com", "USER");
        JwtTokenProvider later = new JwtTokenProvider(secret, 1800,
                Clock.fixed(now.plusSeconds(1801), ZoneOffset.UTC));

        assertThrows(ExpiredJwtException.class, () -> later.parseAndValidate(token));
    }

    @Test
    void rejectsTamperedPayload() {
        String token = provider.createAccessToken(42L, "member@example.com", "USER");
        String[] parts = token.split("\\.");
        String changedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"memberId\":42,\"email\":\"member@example.com\",\"role\":\"ADMIN\"}"
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String tampered = parts[0] + "." + changedPayload + "." + parts[2];

        assertThrows(JwtException.class, () -> provider.parseAndValidate(tampered));
    }

    @Test
    void rejectsTokenSignedWithAnotherKey() {
        String otherSecret = Base64.getEncoder().encodeToString(Jwts.SIG.HS256.key().build().getEncoded());
        String token = new JwtTokenProvider(otherSecret, 1800, clock)
                .createAccessToken(42L, "member@example.com", "USER");

        assertThrows(JwtException.class, () -> provider.parseAndValidate(token));
    }

    @Test
    void rejectsMalformedAndEmptyTokens() {
        assertThrows(JwtException.class, () -> provider.parseAndValidate("not-a-jwt"));
        assertThrows(IllegalArgumentException.class, () -> provider.parseAndValidate(""));
    }

    @Test
    void rejectsSignedTokenWithoutExpiration() {
        String token = Jwts.builder().claim("memberId", 42L)
                .claim("email", "member@example.com").claim("role", "USER")
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret)), Jwts.SIG.HS256).compact();

        assertThrows(JwtException.class, () -> provider.parseAndValidate(token));
    }

    @Test
    void rejectsInvalidConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> new JwtTokenProvider("", 1800));
        assertThrows(WeakKeyException.class,
                () -> new JwtTokenProvider(Base64.getEncoder().encodeToString(new byte[16]), 1800));
        assertThrows(IllegalArgumentException.class, () -> new JwtTokenProvider(secret, 0));
        assertThrows(IllegalArgumentException.class, () -> new JwtTokenProvider(secret, -1));
    }
}