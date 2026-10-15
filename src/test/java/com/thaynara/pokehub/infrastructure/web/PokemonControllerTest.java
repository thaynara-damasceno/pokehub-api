package com.thaynara.pokehub.infrastructure.web;

import com.thaynara.pokehub.application.port.in.ListPokemonUseCase;
import com.thaynara.pokehub.domain.exception.InvalidPaginationException;
import com.thaynara.pokehub.domain.exception.PokemonProviderUnavailableException;
import com.thaynara.pokehub.domain.model.Pokemon;
import com.thaynara.pokehub.domain.model.PokemonPage;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PokemonController.class)
@AutoConfigureMockMvc(addFilters = false)
class PokemonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListPokemonUseCase listPokemonUseCase;

    @Test
    void defaultsToPageZeroAndSizeTwentyWhenParametersAreOmitted() throws Exception {
        when(listPokemonUseCase.list(0, 20)).thenReturn(new PokemonPage(List.of(), 0, 20, 0));

        mockMvc.perform(get("/api/pokemon"))
                .andExpect(status().isOk());

        verify(listPokemonUseCase).list(0, 20);
    }

    @Test
    void passesRequestedPageAndSizeToTheUseCase() throws Exception {
        when(listPokemonUseCase.list(2, 10)).thenReturn(new PokemonPage(List.of(), 2, 10, 0));

        mockMvc.perform(get("/api/pokemon")
                        .param("page", "2")
                        .param("size", "10"))
                .andExpect(status().isOk());

        verify(listPokemonUseCase).list(2, 10);
    }

    @Test
    void returnsPokemonItemsAndPaginationMetadata() throws Exception {
        Pokemon pokemon = new Pokemon(
                1,
                "bulbasaur",
                "https://example.com/bulbasaur.png",
                List.of("grass", "poison"),
                69,
                List.of("overgrow", "chlorophyll"));
        when(listPokemonUseCase.list(0, 2))
                .thenReturn(new PokemonPage(List.of(pokemon), 0, 2, 3));

        mockMvc.perform(get("/api/pokemon").param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items[0].id").value(1))
                .andExpect(jsonPath("$.items[0].name").value("bulbasaur"))
                .andExpect(jsonPath("$.items[0].spriteUrl").value("https://example.com/bulbasaur.png"))
                .andExpect(jsonPath("$.items[0].types[0]").value("grass"))
                .andExpect(jsonPath("$.items[0].types[1]").value("poison"))
                .andExpect(jsonPath("$.items[0].weight").value(69))
                .andExpect(jsonPath("$.items[0].abilities[0]").value("overgrow"))
                .andExpect(jsonPath("$.items[0].abilities[1]").value("chlorophyll"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void returnsAnEmptyItemsArrayAndZeroTotalPagesForAnEmptyPage() throws Exception {
        when(listPokemonUseCase.list(0, 20)).thenReturn(new PokemonPage(List.of(), 0, 20, 0));

        mockMvc.perform(get("/api/pokemon"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    void returnsBadRequestProblemDetailsForInvalidPagination() throws Exception {
        when(listPokemonUseCase.list(0, 20))
                .thenThrow(new InvalidPaginationException("size must be between 1 and 50"));

        mockMvc.perform(get("/api/pokemon"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("size must be between 1 and 50"));
    }

    @Test
    void returnsSanitizedBadGatewayProblemDetailsWhenProviderIsUnavailable() throws Exception {
        when(listPokemonUseCase.list(0, 20))
                .thenThrow(new PokemonProviderUnavailableException("internal provider failure"));

        mockMvc.perform(get("/api/pokemon"))
                .andExpect(status().isBadGateway())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.detail").value(
                        "The Pokémon data provider is currently unavailable. Please try again later."))
                .andExpect(content().string(not(containsString("internal provider failure"))));
    }

    @Test
    void returnsBadRequestProblemDetailsWhenPageParameterIsNotAnInteger() throws Exception {
        mockMvc.perform(get("/api/pokemon").param("page", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.detail").value("Invalid value for parameter 'page'."));
    }

    @Test
    void returnsBadRequestProblemDetailsWhenSizeParameterIsNotAnInteger() throws Exception {
        mockMvc.perform(get("/api/pokemon").param("size", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.detail").value("Invalid value for parameter 'size'."));
    }
}
