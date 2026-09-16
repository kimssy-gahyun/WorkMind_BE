package com.gh.workmind.auth.jwt;

import java.time.Clock;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long expirationSeconds;
    private final Clock clock;
    private final JwtParser parser;

    @Autowired
    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration-seconds}") long expirationSeconds) {
        this(secret, expirationSeconds, Clock.systemUTC());
    }

    JwtTokenProvider(String secret, long expirationSeconds, Clock clock) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("jwt.secret must be a Base64-encoded secret of at least 32 bytes");
        }
        if (expirationSeconds <= 0) {
            throw new IllegalArgumentException("jwt.access-token-expiration-seconds must be positive");
        }
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expirationSeconds = expirationSeconds;
        this.clock = clock;
        this.parser = Jwts.parser()
                .verifyWith(secretKey)
                .sig().clear().add(Jwts.SIG.HS256).and()
                .clock(() -> Date.from(clock.instant()))
                .build();
    }

    public String createAccessToken(Long memberId, String email, String role) {
        if (memberId == null || email == null || email.isBlank() || role == null || role.isBlank()) {
            throw new IllegalArgumentException("memberId, email and role are required");
        }
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(memberId.toString())
                .claim("memberId", memberId)
                .claim("email", email)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationSeconds)))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Verifies the signature and expiration before returning claims.
     * Invalid or expired tokens throw JwtException; empty input throws IllegalArgumentException.
     */
    public Claims parseAndValidate(String token) {
        Claims claims = parser.parseSignedClaims(token).getPayload();
        if (claims.getExpiration() == null || claims.get("memberId", Long.class) == null
                || claims.get("email", String.class) == null || claims.get("role", String.class) == null) {
            throw new MalformedJwtException("Required access token claims are missing");
        }
        return claims;
    }
}