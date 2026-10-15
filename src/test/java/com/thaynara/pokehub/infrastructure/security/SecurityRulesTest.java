package com.thaynara.pokehub.infrastructure.security;

import com.thaynara.pokehub.application.port.in.ListPokemonUseCase;
import com.thaynara.pokehub.domain.model.PokemonPage;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
}
