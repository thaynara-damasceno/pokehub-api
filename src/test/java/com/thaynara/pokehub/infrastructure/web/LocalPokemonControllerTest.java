package com.thaynara.pokehub.infrastructure.web;

import com.thaynara.pokehub.application.port.in.GetLocalPokemonUseCase;
import com.thaynara.pokehub.application.port.in.ListLocalPokemonUseCase;
import com.thaynara.pokehub.application.port.in.SyncPokemonUseCase;
import com.thaynara.pokehub.application.port.in.UpdateLocalPokemonUseCase;
import com.thaynara.pokehub.application.port.in.DeleteLocalPokemonUseCase;
import com.thaynara.pokehub.domain.exception.InvalidLocalPokemonException;
import com.thaynara.pokehub.domain.exception.InvalidPokemonIdException;
import com.thaynara.pokehub.domain.exception.LocalPokemonNotFoundException;
import com.thaynara.pokehub.domain.exception.PokemonNotFoundException;
import com.thaynara.pokehub.domain.exception.PokemonProviderUnavailableException;
import com.thaynara.pokehub.domain.model.LocalPokemon;
import com.thaynara.pokehub.infrastructure.security.SecurityConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LocalPokemonController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class LocalPokemonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SyncPokemonUseCase syncPokemonUseCase;

    @MockitoBean
    private GetLocalPokemonUseCase getLocalPokemonUseCase;

    @MockitoBean
    private ListLocalPokemonUseCase listLocalPokemonUseCase;

    @MockitoBean
    private UpdateLocalPokemonUseCase updateLocalPokemonUseCase;

    @MockitoBean
    private DeleteLocalPokemonUseCase deleteLocalPokemonUseCase;

    @Test
    void syncsPokemonAndReturnsItsLocalFields() throws Exception {
        when(syncPokemonUseCase.sync(1)).thenReturn(pokemon(1));

        mockMvc.perform(post("/api/pokemon/1/sync").with(authenticated()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("bulbasaur"))
                .andExpect(jsonPath("$.imageUrl").value("image-1"))
                .andExpect(jsonPath("$.description").value("description-1"))
                .andExpect(jsonPath("$.localizedName").value("Nombre local"))
                .andExpect(jsonPath("$.region").value("Kanto"))
                .andExpect(jsonPath("$.tags[0]").value("favorite"))
                .andExpect(jsonPath("$.tags[1]").value("starter"));

        verify(syncPokemonUseCase).sync(1);
    }

    @Test
    void returnsLocalPokemonAsAnArray() throws Exception {
        when(listLocalPokemonUseCase.list()).thenReturn(List.of(pokemon(1)));

        mockMvc.perform(get("/api/local-pokemon").with(authenticated()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void returnsLocalPokemonByIdWithAllFields() throws Exception {
        when(getLocalPokemonUseCase.getById(1)).thenReturn(pokemon(1));

        mockMvc.perform(get("/api/local-pokemon/1").with(authenticated()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("bulbasaur"))
                .andExpect(jsonPath("$.imageUrl").value("image-1"))
                .andExpect(jsonPath("$.description").value("description-1"))
                .andExpect(jsonPath("$.localizedName").value("Nombre local"))
                .andExpect(jsonPath("$.region").value("Kanto"))
                .andExpect(jsonPath("$.tags[0]").value("favorite"))
                .andExpect(jsonPath("$.tags[1]").value("starter"));
    }

    @Test
    void returnsNotFoundProblemDetailsWhenLocalPokemonDoesNotExist() throws Exception {
        when(getLocalPokemonUseCase.getById(999))
                .thenThrow(new LocalPokemonNotFoundException(999));

        mockMvc.perform(get("/api/local-pokemon/999").with(authenticated()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.detail")
                        .value("Local Pokémon with id 999 was not found."));
    }

    @Test
    void mapsSyncUseCaseExceptionsToTheirExistingProblemDetails() throws Exception {
        when(syncPokemonUseCase.sync(404)).thenThrow(new PokemonNotFoundException(404));
        when(syncPokemonUseCase.sync(400))
                .thenThrow(new InvalidPokemonIdException("Invalid Pokémon id."));
        when(syncPokemonUseCase.sync(502))
                .thenThrow(new PokemonProviderUnavailableException("internal failure"));

        mockMvc.perform(post("/api/pokemon/404/sync").with(authenticated()))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/pokemon/400/sync").with(authenticated()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/pokemon/502/sync").with(authenticated()))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value(
                        "The Pokémon data provider is currently unavailable. Please try again later."))
                .andExpect(content().string(not(containsString("internal failure"))));
    }

    @Test
    void requiresAuthenticationForSyncAndLocalPokemonRoutes() throws Exception {
        mockMvc.perform(post("/api/pokemon/1/sync"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/local-pokemon"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/local-pokemon/1"))
                .andExpect(status().isUnauthorized());
    }

    private RequestPostProcessor authenticated() {
        return SecurityMockMvcRequestPostProcessors.user("test-user");
    }

    private LocalPokemon pokemon(int id) {
        return new LocalPokemon(
                id,
                "bulbasaur",
                "image-" + id,
                "description-" + id,
                "Nombre local",
                "Kanto",
                List.of("favorite", "starter"));
    }
}
