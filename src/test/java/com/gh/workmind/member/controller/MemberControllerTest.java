package com.gh.workmind.member.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.gh.workmind.auth.JwtTestSupport;
import com.gh.workmind.auth.jwt.JwtTokenProvider;
import com.gh.workmind.config.SecurityConfig;
import com.gh.workmind.member.model.entity.Member;
import com.gh.workmind.member.model.repository.MemberRepository;
import com.gh.workmind.member.model.service.MemberService;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.Filter;
import tools.jackson.databind.ObjectMapper;

@SpringJUnitWebConfig(MemberControllerTest.TestConfig.class)
class MemberControllerTest {

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({SecurityConfig.class, JwtTokenProvider.class, MemberController.class, MemberService.class})
    static class TestConfig {
        @Bean
        MemberRepository memberRepository() {
            return mock(MemberRepository.class);
        }
    }

    @DynamicPropertySource
    static void jwtProperties(DynamicPropertyRegistry registry) {
        JwtTestSupport.configure(registry);
    }

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private MemberRepository memberRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(memberRepository);
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean("springSecurityFilterChain", Filter.class)).build();
    }

    @Test
    void returnsOnlyCurrentDatabaseFieldsUsingAuthenticatedMemberId() throws Exception {
        Member member = member();
        when(memberRepository.findById(42L)).thenReturn(Optional.of(member));
        // JWT values intentionally differ from the current database values.
        String token = jwtTokenProvider.createAccessToken(42L, "old@example.com", "USER");

        mockMvc.perform(get("/workmind/members/me").contextPath("/workmind")
                        .param("memberId", "999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.memberId").value(42))
                .andExpect(jsonPath("$.email").value("current@example.com"))
                .andExpect(jsonPath("$.name").value("Current Member"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.enrollDate").value("2026-09-16T10:00:00"))
                .andExpect(result -> {
                    Map<?, ?> body = new ObjectMapper().readValue(
                            result.getResponse().getContentAsString(), Map.class);
                    assertEquals(Set.of("memberId", "email", "name", "role", "enrollDate", "modifyDate"),
                            body.keySet());
                    assertEquals(null, body.get("modifyDate"));
                    assertFalse(result.getResponse().getContentAsString().contains(member.getPassword()));
                });
        verify(memberRepository).findById(42L);
        verifyNoMoreInteractions(memberRepository);
    }

    @Test
    void returnsDatabaseModificationDate() throws Exception {
        Member member = member();
        member.setModifyDate(LocalDateTime.of(2026, 9, 16, 11, 30));
        when(memberRepository.findById(42L)).thenReturn(Optional.of(member));

        mockMvc.perform(get("/workmind/members/me").contextPath("/workmind")
                        .header("Authorization", "Bearer " + token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modifyDate").value("2026-09-16T11:30:00"));
    }

    @Test
    void missingDatabaseMemberReturns404() throws Exception {
        when(memberRepository.findById(42L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/workmind/members/me").contextPath("/workmind")
                        .header("Authorization", "Bearer " + token()))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
        verify(memberRepository).findById(42L);
    }

    @Test
    void missingTokenReturns401BeforeDatabaseLookup() throws Exception {
        mockMvc.perform(get("/workmind/members/me").contextPath("/workmind"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));
        verifyNoInteractions(memberRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bearer invalid-token", "Bearer ", "Basic invalid"})
    void invalidTokenReturns401BeforeDatabaseLookup(String authorization) throws Exception {
        mockMvc.perform(get("/workmind/members/me").contextPath("/workmind")
                        .header("Authorization", authorization))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));
        verifyNoInteractions(memberRepository);
    }

    @Test
    void expiredTokenReturns401BeforeDatabaseLookup() throws Exception {
        String expired = Jwts.builder().claim("memberId", 42L)
                .claim("email", "old@example.com").claim("role", "USER")
                .expiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(JwtTestSupport.SECRET)),
                        Jwts.SIG.HS256).compact();

        mockMvc.perform(get("/workmind/members/me").contextPath("/workmind")
                        .header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));
        verifyNoInteractions(memberRepository);
    }

    private String token() {
        return jwtTokenProvider.createAccessToken(42L, "old@example.com", "USER");
    }

    private Member member() {
        Member member = new Member();
        member.setMemberId(42L);
        member.setEmail("current@example.com");
        member.setName("Current Member");
        member.setRole("ADMIN");
        member.setPassword("password-must-never-appear");
        member.setEnrollDate(LocalDateTime.of(2026, 9, 16, 10, 0));
        return member;
    }
}