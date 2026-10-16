package com.thaynara.pokehub.infrastructure.security;

import com.thaynara.pokehub.application.model.AccessToken;
import com.thaynara.pokehub.application.port.in.ListPokemonUseCase;
import com.thaynara.pokehub.application.usecase.AuthService;
import com.thaynara.pokehub.application.usecase.LocalPokemonService;
import com.thaynara.pokehub.domain.model.PokemonPage;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(JwtAuthenticationTest.BasicAuthenticationTestConfig.class)
class JwtAuthenticationTest {

    private static final String USERNAME = "trainer";
    private static final String PASSWORD = "trainer-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Clock clock;

    @Value("${pokehub.jwt.secret}")
    private String jwtSecret;

    @MockitoBean
    private LocalPokemonService localPokemonService;

    @MockitoBean
    private ListPokemonUseCase listPokemonUseCase;

    @MockitoBean
    private AuthService authService;

    @BeforeEach
    void configureMockedUseCases() {
        when(localPokemonService.list()).thenReturn(List.of());
        when(listPokemonUseCase.list(0, 20))
                .thenReturn(new PokemonPage(List.of(), 0, 20, 0));
        when(authService.authenticate(USERNAME, PASSWORD))
                .thenReturn(new AccessToken("issued-token", 3600));
    }

    @Test
    void acceptsValidBearerTokenForProtectedRoute() throws Exception {
        mockMvc.perform(get("/api/local-pokemon")
                        .header("Authorization", "Bearer " + issueToken(jwtSecret, clock, 3600)))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsProtectedRouteWithoutAuthorizationHeader() throws Exception {
        mockMvc.perform(get("/api/local-pokemon"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsGarbageBearerToken() throws Exception {
        mockMvc.perform(get("/api/local-pokemon")
                        .header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsBearerTokenSignedWithDifferentSecret() throws Exception {
        mockMvc.perform(get("/api/local-pokemon")
                        .header("Authorization", "Bearer "
                                + issueToken("another-test-secret-with-32-bytes", clock, 3600)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsExpiredBearerToken() throws Exception {
        Clock pastClock = Clock.fixed(Instant.now().minusSeconds(120), ZoneOffset.UTC);
        mockMvc.perform(get("/api/local-pokemon")
                        .header("Authorization", "Bearer "
                                + issueToken(jwtSecret, pastClock, 60)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsValidHttpBasicCredentials() throws Exception {
        String credentials = Base64.getEncoder().encodeToString(
                "basic-user:basic-password".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(get("/api/local-pokemon")
                        .header("Authorization", "Basic " + credentials))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void leavesPokemonListingAndLoginPublic() throws Exception {
        mockMvc.perform(get("/api/pokemon"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"username":"trainer","password":"trainer-password"}
                                """))
                .andExpect(status().isOk());
    }

    private String issueToken(String secret, Clock tokenClock, long ttlSeconds) {
        return new JwtTokenIssuer(secret, ttlSeconds, tokenClock).issue(USERNAME).value();
    }

    @TestConfiguration
    static class BasicAuthenticationTestConfig {

        @Bean
        UserDetailsService testUserDetailsService() {
            return new InMemoryUserDetailsManager(
                    User.withUsername("basic-user")
                            .password("{noop}basic-password")
                            .roles("USER")
                            .build());
        }
    }
}
