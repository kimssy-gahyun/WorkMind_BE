package com.gh.workmind.auth.model.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.gh.workmind.auth.model.dto.LoginRequest;
import com.gh.workmind.auth.jwt.JwtTokenProvider;
import com.gh.workmind.member.model.entity.Member;
import com.gh.workmind.member.model.repository.MemberRepository;

class AuthServiceTest {

    private MemberRepository memberRepository;
    private BCryptPasswordEncoder passwordEncoder;
    private AuthService authService;
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        memberRepository = mock(MemberRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        jwtTokenProvider = mock(JwtTokenProvider.class);
        authService = new AuthService(memberRepository, passwordEncoder, jwtTokenProvider);
    }

    @Test
    void acceptsMatchingBcryptPassword() {
        Member member = new Member();
        member.setMemberId(42L);
        member.setEmail("member@example.com");
        member.setRole("USER");
        member.setPassword(passwordEncoder.encode("correct-password"));
        when(memberRepository.findByEmail("member@example.com")).thenReturn(Optional.of(member));

        when(jwtTokenProvider.createAccessToken(42L, "member@example.com", "USER")).thenReturn("access-token");
        assertEquals("access-token", authService.login(request("member@example.com", "correct-password")).accessToken());
        verify(jwtTokenProvider).createAccessToken(42L, "member@example.com", "USER");
        verify(memberRepository).findByEmail("member@example.com");
    }

    @Test
    void rejectsUnknownEmail() {
        when(memberRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class,
                () -> authService.login(request("missing@example.com", "password")));
        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    void rejectsWrongPassword() {
        Member member = new Member();
        member.setMemberId(42L);
        member.setEmail("member@example.com");
        member.setRole("USER");
        member.setPassword(passwordEncoder.encode("correct-password"));
        when(memberRepository.findByEmail("member@example.com")).thenReturn(Optional.of(member));

        assertThrows(BadCredentialsException.class,
                () -> authService.login(request("member@example.com", "wrong-password")));
        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    void rejectsMissingAndBlankCredentialsBeforeLookup() {
        assertThrows(BadCredentialsException.class, () -> authService.login(null));
        assertThrows(BadCredentialsException.class, () -> authService.login(request(null, "password")));
        assertThrows(BadCredentialsException.class, () -> authService.login(request(" ", "password")));
        assertThrows(BadCredentialsException.class, () -> authService.login(request("member@example.com", null)));
        assertThrows(BadCredentialsException.class, () -> authService.login(request("member@example.com", " ")));
        verifyNoInteractions(memberRepository, jwtTokenProvider);
    }

    private LoginRequest request(String email, String password) {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }
}