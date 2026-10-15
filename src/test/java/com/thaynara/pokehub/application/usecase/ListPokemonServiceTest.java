package com.thaynara.pokehub.application.usecase;

import com.thaynara.pokehub.application.port.out.PokemonGateway;
import com.thaynara.pokehub.domain.exception.InvalidPaginationException;
import com.thaynara.pokehub.domain.model.PokemonPage;
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
class ListPokemonServiceTest {

    @Mock
    private PokemonGateway pokemonGateway;

    @InjectMocks
    private ListPokemonService listPokemonService;

    @Test
    void returnsThePageProvidedByTheGateway() {
        PokemonPage expectedPage = new PokemonPage(List.of(), 0, 20, 0);
        when(pokemonGateway.findPage(0, 20)).thenReturn(expectedPage);

        PokemonPage result = listPokemonService.list(0, 20);

        assertThat(result).isSameAs(expectedPage);
        verify(pokemonGateway).findPage(0, 20);
    }

    @Test
    void rejectsANegativePageWithoutCallingTheGateway() {
        assertThatThrownBy(() -> listPokemonService.list(-1, 20))
                .isInstanceOf(InvalidPaginationException.class);

        verifyNoInteractions(pokemonGateway);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -5, 51})
    void rejectsAnInvalidPageSizeWithoutCallingTheGateway(int size) {
        assertThatThrownBy(() -> listPokemonService.list(0, size))
                .isInstanceOf(InvalidPaginationException.class);

        verifyNoInteractions(pokemonGateway);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 50})
    void acceptsTheMinimumAndMaximumPageSizes(int size) {
        PokemonPage expectedPage = new PokemonPage(List.of(), 0, size, 0);
        when(pokemonGateway.findPage(0, size)).thenReturn(expectedPage);

        PokemonPage result = listPokemonService.list(0, size);

        assertThat(result).isSameAs(expectedPage);
        verify(pokemonGateway).findPage(0, size);
    }
}
