package com.thaynara.pokehub.infrastructure.security;

import com.thaynara.pokehub.application.port.in.ListPokemonUseCase;
import com.thaynara.pokehub.domain.model.PokemonPage;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityRulesTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListPokemonUseCase listPokemonUseCase;

    @BeforeEach
    void returnAnEmptyPokemonPage() {
        when(listPokemonUseCase.list(0, 20))
                .thenReturn(new PokemonPage(List.of(), 0, 20, 0));
    }

    @Test
    void allowsUnauthenticatedPokemonListing() throws Exception {
        mockMvc.perform(get("/api/pokemon"))
                .andExpect(status().isOk());
    }

    @Test
    void allowsUnauthenticatedOpenApiDocs() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    void allowsUnauthenticatedSwaggerUi() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsUnauthenticatedPokemonSyncWritesWithUnauthorized() throws Exception {
        mockMvc.perform(post("/api/pokemon/1/sync"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsUnauthenticatedLocalPokemonAccess() throws Exception {
        mockMvc.perform(get("/api/local-pokemon"))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("unauthorizedBearerCases")
    void returnsProblemDetailsForBearerAuthenticationFailures(
            String displayName, String token) throws Exception {
        MockHttpServletRequestBuilder request = get("/api/local-pokemon");
        if (token != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }

        mockMvc.perform(request)
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(
                        HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")))
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").isNotEmpty());
    }

    private static Stream<Arguments> unauthorizedBearerCases() {
        return Stream.of(
                Arguments.of("local listing: missing bearer token", null),
                Arguments.of("local listing: invalid bearer token", "garbage-token"));
    }
}
