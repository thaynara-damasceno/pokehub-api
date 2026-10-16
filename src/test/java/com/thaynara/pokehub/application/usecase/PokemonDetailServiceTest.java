package com.thaynara.pokehub.application.usecase;

import com.thaynara.pokehub.application.port.out.PokemonGateway;
import com.thaynara.pokehub.domain.exception.InvalidPokemonIdException;
import com.thaynara.pokehub.domain.exception.PokemonNotFoundException;
import com.thaynara.pokehub.domain.model.PokemonDetail;
import com.thaynara.pokehub.domain.model.PokemonStat;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PokemonDetailServiceTest {

    @Mock
    private PokemonGateway pokemonGateway;

    @InjectMocks
    private PokemonDetailService pokemonDetailService;

    @ParameterizedTest
    @ValueSource(ints = {1, 25})
    void returnsTheDetailProvidedByTheGatewayForAValidPokemonId(int id) {
        PokemonDetail expectedDetail = new PokemonDetail(
                id, "bulbasaur", "image", List.of(new PokemonStat("hp", 45)), "description",
                List.of("bulbasaur"));
        when(pokemonGateway.findById(id)).thenReturn(expectedDetail);

        PokemonDetail result = pokemonDetailService.get(id);

        assertThat(result).isSameAs(expectedDetail);
        verify(pokemonGateway).findById(id);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsPokemonIdsBelowOneWithoutCallingTheGateway(int id) {
        assertThatThrownBy(() -> pokemonDetailService.get(id))
                .isInstanceOf(InvalidPokemonIdException.class);

        verifyNoInteractions(pokemonGateway);
    }

    @Test
    void propagatesPokemonNotFoundFromTheGateway() {
        when(pokemonGateway.findById(999)).thenThrow(new PokemonNotFoundException(999));

        assertThatThrownBy(() -> pokemonDetailService.get(999))
                .isInstanceOf(PokemonNotFoundException.class)
                .hasMessage("Pokémon with id 999 was not found.");

        verify(pokemonGateway).findById(999);
    }
}
