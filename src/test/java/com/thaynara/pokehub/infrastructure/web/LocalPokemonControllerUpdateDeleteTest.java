package com.thaynara.pokehub.infrastructure.web;

import com.thaynara.pokehub.application.port.in.DeleteLocalPokemonUseCase;
import com.thaynara.pokehub.application.port.in.GetLocalPokemonUseCase;
import com.thaynara.pokehub.application.port.in.ListLocalPokemonUseCase;
import com.thaynara.pokehub.application.port.in.SyncPokemonUseCase;
import com.thaynara.pokehub.application.port.in.UpdateLocalPokemonUseCase;
import com.thaynara.pokehub.domain.exception.InvalidLocalPokemonException;
import com.thaynara.pokehub.domain.exception.LocalPokemonNotFoundException;
import com.thaynara.pokehub.domain.model.LocalPokemon;
import com.thaynara.pokehub.infrastructure.security.SecurityConfig;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LocalPokemonController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class LocalPokemonControllerUpdateDeleteTest {

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
    void updatesLocalPokemonAndReturnsAllFields() throws Exception {
        when(updateLocalPokemonUseCase.update(25, "Pikachu local", "Kanto", List.of("favorite")))
                .thenReturn(pokemon());

        mockMvc.perform(put("/api/local-pokemon/25")
                        .with(authenticated())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"localizedName":"Pikachu local","region":"Kanto","tags":["favorite"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(25))
                .andExpect(jsonPath("$.name").value("pikachu"))
                .andExpect(jsonPath("$.imageUrl").value("image-25"))
                .andExpect(jsonPath("$.description").value("description-25"))
                .andExpect(jsonPath("$.localizedName").value("Pikachu local"))
                .andExpect(jsonPath("$.region").value("Kanto"))
                .andExpect(jsonPath("$.tags[0]").value("favorite"));

        verify(updateLocalPokemonUseCase)
                .update(25, "Pikachu local", "Kanto", List.of("favorite"));
    }

    @Test
    void updatesLocalPokemonWithNullRegionAndTags() throws Exception {
        when(updateLocalPokemonUseCase.update(25, "Pikachu local", null, null))
                .thenReturn(pokemon());

        mockMvc.perform(put("/api/local-pokemon/25")
                        .with(authenticated())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"localizedName":"Pikachu local","region":null,"tags":null}
                                """))
                .andExpect(status().isOk());

        verify(updateLocalPokemonUseCase).update(25, "Pikachu local", null, null);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("updateValidationCases")
    void rejectsInvalidUpdateRequest(String displayName, String requestBody, String field)
            throws Exception {
        when(updateLocalPokemonUseCase.update(anyInt(), any(), any(), any()))
                .thenReturn(pokemon());
        assertUpdateValidationError(requestBody, field);
        verifyNoInteractions(updateLocalPokemonUseCase);
    }

    private static Stream<Arguments> updateValidationCases() {
        return Stream.of(
                Arguments.of("update: localizedName longer than 60", """
                        {"localizedName":"%s","region":"Kanto","tags":[]}
                        """.formatted("x".repeat(61)), "localizedName"),
                Arguments.of("update: region longer than 60", """
                        {"localizedName":"Pikachu local","region":"%s","tags":[]}
                        """.formatted("x".repeat(61)), "region"),
                Arguments.of("update: more than 10 tags", """
                        {"localizedName":"Pikachu local","region":"Kanto",
                         "tags":["tag","tag","tag","tag","tag","tag","tag","tag","tag","tag","tag"]}
                        """, "tags"),
                Arguments.of("update: blank tag", """
                        {"localizedName":"Pikachu local","region":"Kanto","tags":[" "]}
                        """, "tags[0]"),
                Arguments.of("update: tag longer than 30", """
                        {"localizedName":"Pikachu local","region":"Kanto","tags":["%s"]}
                        """.formatted("x".repeat(31)), "tags[0]"),
                Arguments.of("update: null tag", """
                        {"localizedName":"Pikachu local","region":"Kanto","tags":[null]}
                        """, "tags[0]"));
    }

    @Test
    void returnsNotFoundProblemDetailsWhenUpdatingUnknownLocalPokemon() throws Exception {
        when(updateLocalPokemonUseCase.update(999, null, null, List.of()))
                .thenThrow(new LocalPokemonNotFoundException(999));

        mockMvc.perform(put("/api/local-pokemon/999")
                        .with(authenticated())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"localizedName":null,"region":null,"tags":[]}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.detail")
                        .value("Local Pokémon with id 999 was not found."));
    }

    @Test
    void returnsBadRequestProblemDetailsWhenUpdateDataIsInvalid() throws Exception {
        when(updateLocalPokemonUseCase.update(25, "Invalid", "Kanto", List.of()))
                .thenThrow(new InvalidLocalPokemonException("Localized name is too long."));

        mockMvc.perform(put("/api/local-pokemon/25")
                        .with(authenticated())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"localizedName":"Invalid","region":"Kanto","tags":[]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.detail").value("Localized name is too long."));
    }

    @Test
    void deletesLocalPokemonAndReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/local-pokemon/25").with(authenticated()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(deleteLocalPokemonUseCase).delete(25);
    }

    @Test
    void returnsNotFoundProblemDetailsWhenDeletingUnknownLocalPokemon() throws Exception {
        doThrow(new LocalPokemonNotFoundException(999))
                .when(deleteLocalPokemonUseCase).delete(999);

        mockMvc.perform(delete("/api/local-pokemon/999").with(authenticated()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.detail")
                        .value("Local Pokémon with id 999 was not found."));
    }

    @Test
    void requiresAuthenticationForUpdateAndDeleteRoutes() throws Exception {
        mockMvc.perform(put("/api/local-pokemon/25")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"localizedName":"Pikachu local","region":"Kanto","tags":[]}
                                """))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/local-pokemon/25"))
                .andExpect(status().isUnauthorized());
    }

    private RequestPostProcessor authenticated() {
        return SecurityMockMvcRequestPostProcessors.user("test-user");
    }

    private LocalPokemon pokemon() {
        return new LocalPokemon(
                25, "pikachu", "image-25", "description-25",
                "Pikachu local", "Kanto", List.of("favorite"));
    }

    private void assertUpdateValidationError(String requestBody, String field)
            throws Exception {
        mockMvc.perform(put("/api/local-pokemon/25")
                        .with(authenticated())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0].field").value(field))
                .andExpect(jsonPath("$.errors[0].message").isNotEmpty());
    }
}
