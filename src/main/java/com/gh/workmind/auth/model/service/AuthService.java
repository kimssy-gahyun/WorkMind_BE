package com.gh.workmind.auth.model.service;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gh.workmind.auth.model.dto.LoginRequest;
import com.gh.workmind.auth.model.dto.LoginResponse;
import com.gh.workmind.auth.jwt.JwtTokenProvider;
import com.gh.workmind.member.model.entity.Member;
import com.gh.workmind.member.model.repository.MemberRepository;

@Service
public class AuthService {

    private final MemberRepository memberRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(MemberRepository memberRepository, BCryptPasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        if (request == null || request.getEmail() == null || request.getEmail().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank()) {
            throw invalidCredentials();
        }

        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(this::invalidCredentials);

        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw invalidCredentials();
        }

        return new LoginResponse(jwtTokenProvider.createAccessToken(
                member.getMemberId(), member.getEmail(), member.getRole()));
    }

    private BadCredentialsException invalidCredentials() {
        return new BadCredentialsException("Invalid email or password");
    }
}