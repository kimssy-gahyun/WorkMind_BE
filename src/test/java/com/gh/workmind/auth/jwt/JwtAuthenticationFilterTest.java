package com.gh.workmind.auth.jwt;


import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.util.Date;
import java.util.Map;

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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.gh.workmind.auth.JwtTestSupport;
import com.gh.workmind.config.SecurityConfig;
import com.gh.workmind.member.controller.MemberController;
import com.gh.workmind.member.model.entity.Member;
import com.gh.workmind.member.model.repository.MemberRepository;
import com.gh.workmind.member.model.service.MemberService;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.Filter;

@SpringJUnitWebConfig(JwtAuthenticationFilterTest.TestConfig.class)
class JwtAuthenticationFilterTest {

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({SecurityConfig.class, JwtTokenProvider.class, ProbeController.class, MemberController.class, MemberService.class})
    static class TestConfig {
        @Bean
        MemberRepository memberRepository() {
            return mock(MemberRepository.class);
        }
    }

    // Test-only endpoint: never included in the production application.
    @RestController
    static class ProbeController {
        @GetMapping("/test/authentication")
        Map<String, Object> authentication() {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
            assertNull(authentication.getCredentials());
            return Map.of(
                    "memberId", principal.memberId(),
                    "email", principal.email(),
                    "role", principal.role(),
                    "authority", authentication.getAuthorities().iterator().next().getAuthority(),
                    "authenticated", authentication.isAuthenticated());
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

    @ParameterizedTest
    @ValueSource(strings = {"USER", "ADMIN"})
    void validTokenPopulatesContextAndAuthorityWithoutDatabaseOrSession(String role) throws Exception {
        String token = jwtTokenProvider.createAccessToken(42L, "member@example.com", role);
        mockMvc.perform(get("/workmind/test/authentication").contextPath("/workmind")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(42))
                .andExpect(jsonPath("$.email").value("member@example.com"))
                .andExpect(jsonPath("$.role").value(role))
                .andExpect(jsonPath("$.authority").value("ROLE_" + role))
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(result -> assertNull(result.getRequest().getSession(false)));
        verifyNoInteractions(memberRepository);
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        // A previous valid request must not authenticate the next request.
        mockMvc.perform(get("/workmind/test/authentication").contextPath("/workmind"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void missingHeaderReturns401WithoutSessionOrRedirect() throws Exception {
        mockMvc.perform(get("/workmind/test/authentication").contextPath("/workmind"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(header().doesNotExist("Location"))
                .andExpect(result -> assertNull(result.getRequest().getSession(false)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bearer invalid-token", "Bearer ", "Basic abc", "Bearer"})
    void invalidOrUnsupportedHeaderReturns401(String authorization) throws Exception {
        mockMvc.perform(get("/workmind/test/authentication").contextPath("/workmind")
                        .header("Authorization", authorization))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredTokenReturns401() throws Exception {
        String token = signedClaims(Instant.now().minusSeconds(60), 42L, "USER");
        mockMvc.perform(get("/workmind/test/authentication").contextPath("/workmind")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongSignatureReturns401() throws Exception {
        String token = Jwts.builder().claim("memberId", 42L).claim("email", "member@example.com")
                .claim("role", "USER").expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(Jwts.SIG.HS256.key().build()).compact();
        mockMvc.perform(get("/workmind/test/authentication").contextPath("/workmind")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidClaimTypeReturns401() throws Exception {
        String token = signedClaims(Instant.now().plusSeconds(300), "not-a-number", "USER");
        mockMvc.perform(get("/workmind/test/authentication").contextPath("/workmind")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unsupportedRoleReturns401() throws Exception {
        String token = signedClaims(Instant.now().plusSeconds(300), 42L, "ROOT");
        mockMvc.perform(get("/workmind/test/authentication").contextPath("/workmind")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void signupRemainsPublicWithoutToken() throws Exception {
        mockMvc.perform(post("/workmind/members").contextPath("/workmind")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"new@example.com\",\"password\":\"password\",\"name\":\"New\"}"))
                .andExpect(status().isCreated());
        verify(memberRepository).save(any(Member.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid-token", "expired"})
    void invalidTokenDoesNotBlockPublicSignup(String value) throws Exception {
        String token = value.equals("expired")
                ? signedClaims(Instant.now().minusSeconds(60), 42L, "USER") : value;
        mockMvc.perform(post("/workmind/members").contextPath("/workmind")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"new@example.com\",\"password\":\"password\",\"name\":\"New\"}"))
                .andExpect(status().isCreated());
        verify(memberRepository).save(any(Member.class));
    }

    @Test
    void onlyPostMembersIsPublic() throws Exception {
        mockMvc.perform(get("/workmind/members").contextPath("/workmind"))
                .andExpect(status().isUnauthorized());
    }

    private String signedClaims(Instant expiration, Object memberId, String role) {
        return Jwts.builder().claim("memberId", memberId).claim("email", "member@example.com")
                .claim("role", role).expiration(Date.from(expiration))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(JwtTestSupport.SECRET)), Jwts.SIG.HS256)
                .compact();
    }
}