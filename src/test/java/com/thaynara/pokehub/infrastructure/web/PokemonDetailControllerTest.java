package com.thaynara.pokehub.infrastructure.web;

import com.thaynara.pokehub.application.port.in.GetPokemonDetailUseCase;
import com.thaynara.pokehub.domain.exception.InvalidPokemonIdException;
import com.thaynara.pokehub.domain.exception.PokemonNotFoundException;
import com.thaynara.pokehub.domain.exception.PokemonProviderUnavailableException;
import com.thaynara.pokehub.domain.model.PokemonDetail;
import com.thaynara.pokehub.domain.model.PokemonStat;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PokemonDetailController.class)
@AutoConfigureMockMvc(addFilters = false)
class PokemonDetailControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetPokemonDetailUseCase getPokemonDetailUseCase;

    @Test
    void returnsPokemonDetailFieldsForAValidId() throws Exception {
        when(getPokemonDetailUseCase.get(1)).thenReturn(new PokemonDetail(
                1,
                "bulbasaur",
                "https://example.com/bulbasaur.png",
                List.of(new PokemonStat("hp", 45)),
                "A strange seed was planted on its back.",
                List.of("bulbasaur", "ivysaur", "venusaur")));

        mockMvc.perform(get("/api/pokemon/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("bulbasaur"))
                .andExpect(jsonPath("$.imageUrl").value("https://example.com/bulbasaur.png"))
                .andExpect(jsonPath("$.stats[0].name").value("hp"))
                .andExpect(jsonPath("$.stats[0].value").value(45))
                .andExpect(jsonPath("$.description").value("A strange seed was planted on its back."))
                .andExpect(jsonPath("$.evolutionChain[0]").value("bulbasaur"))
                .andExpect(jsonPath("$.evolutionChain[1]").value("ivysaur"))
                .andExpect(jsonPath("$.evolutionChain[2]").value("venusaur"));
    }

    @Test
    void returnsNotFoundProblemDetailsWhenPokemonDoesNotExist() throws Exception {
        when(getPokemonDetailUseCase.get(999)).thenThrow(new PokemonNotFoundException(999));

        mockMvc.perform(get("/api/pokemon/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Pokémon with id 999 was not found."));
    }

    @Test
    void returnsBadRequestProblemDetailsWhenPokemonIdIsInvalid() throws Exception {
        when(getPokemonDetailUseCase.get(0)).thenThrow(
                new InvalidPokemonIdException("Pokémon id must be greater than or equal to 1."));

        mockMvc.perform(get("/api/pokemon/0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.detail")
                        .value("Pokémon id must be greater than or equal to 1."));
    }

    @Test
    void returnsSanitizedBadGatewayProblemDetailsWhenProviderIsUnavailable() throws Exception {
        when(getPokemonDetailUseCase.get(1))
                .thenThrow(new PokemonProviderUnavailableException("internal failure"));

        mockMvc.perform(get("/api/pokemon/1"))
                .andExpect(status().isBadGateway())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.detail").value(
                        "The Pokémon data provider is currently unavailable. Please try again later."))
                .andExpect(content().string(not(containsString("internal failure"))));
    }
}
