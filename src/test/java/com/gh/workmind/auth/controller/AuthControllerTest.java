package com.gh.workmind.auth.controller;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.gh.workmind.auth.model.service.AuthService;
import com.gh.workmind.auth.jwt.JwtTokenProvider;
import com.gh.workmind.auth.JwtTestSupport;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import io.jsonwebtoken.Claims;
import tools.jackson.databind.ObjectMapper;
import com.gh.workmind.config.SecurityConfig;
import com.gh.workmind.member.model.entity.Member;
import com.gh.workmind.member.model.repository.MemberRepository;

import jakarta.servlet.Filter;

@SpringJUnitWebConfig(AuthControllerTest.TestConfig.class)
class AuthControllerTest {

    @DynamicPropertySource
    static void jwtProperties(DynamicPropertyRegistry registry) {
        JwtTestSupport.configure(registry);
    }

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({SecurityConfig.class, AuthController.class, AuthService.class, JwtTokenProvider.class})
    static class TestConfig {

        @Bean
        MemberRepository memberRepository() {
            return mock(MemberRepository.class);
        }
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(memberRepository);
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean("springSecurityFilterChain", Filter.class))
                .build();
    }

    @Test
    void anonymousLoginReturnsVerifiedAccessTokenWithoutSession() throws Exception {
        existingMember();

        mockMvc.perform(post("/workmind/auth/login").contextPath("/workmind")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"member@example.com","password":"correct-password"}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(result -> {
                    String token = new ObjectMapper().readValue(result.getResponse().getContentAsString(), com.gh.workmind.auth.model.dto.LoginResponse.class).accessToken();
                    Claims claims = jwtTokenProvider.parseAndValidate(token);
                    assertEquals(42L, claims.get("memberId", Long.class));
                    assertEquals("member@example.com", claims.get("email", String.class));
                    assertEquals("USER", claims.get("role", String.class));
                })
                .andExpect(result -> assertNull(result.getRequest().getSession(false)));
    }

    @Test
    void unknownEmailReturnsUnauthorized() throws Exception {
        when(memberRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        mockMvc.perform(post("/workmind/auth/login").contextPath("/workmind")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"missing@example.com","password":"password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));
    }

    @Test
    void wrongPasswordReturnsUnauthorized() throws Exception {
        existingMember();

        mockMvc.perform(post("/workmind/auth/login").contextPath("/workmind")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"member@example.com","password":"wrong-password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));
    }

    @Test
    void missingCredentialsReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/workmind/auth/login").contextPath("/workmind")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void malformedJsonReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/workmind/auth/login").contextPath("/workmind")
                        .contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginGetIsProtected() throws Exception {
        mockMvc.perform(get("/workmind/auth/login").contextPath("/workmind"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void memberProfileRemainsProtected() throws Exception {
        mockMvc.perform(get("/workmind/members/me").contextPath("/workmind"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidBearerDoesNotBlockValidLogin() throws Exception {
        existingMember();
        mockMvc.perform(post("/workmind/auth/login").contextPath("/workmind")
                        .header("Authorization", "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"member@example.com","password":"correct-password"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }
    private void existingMember() {
        Member member = new Member();
        member.setMemberId(42L);
        member.setEmail("member@example.com");
        member.setRole("USER");
        member.setPassword(passwordEncoder.encode("correct-password"));
        when(memberRepository.findByEmail("member@example.com")).thenReturn(Optional.of(member));
    }
}