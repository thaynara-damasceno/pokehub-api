package com.thaynara.pokehub.application.usecase;

import com.thaynara.pokehub.application.port.in.GetPokemonDetailUseCase;
import com.thaynara.pokehub.application.port.out.LocalPokemonRepository;
import com.thaynara.pokehub.domain.exception.LocalPokemonNotFoundException;
import com.thaynara.pokehub.domain.model.LocalPokemon;
import com.thaynara.pokehub.domain.model.PokemonDetail;
import com.thaynara.pokehub.domain.model.PokemonStat;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalPokemonServiceTest {

    @Mock
    private GetPokemonDetailUseCase getPokemonDetailUseCase;

    @Mock
    private LocalPokemonRepository localPokemonRepository;

    @InjectMocks
    private LocalPokemonService localPokemonService;

    @Test
    void syncCreatesAndSavesANewLocalPokemonFromTheProviderDetail() {
        PokemonDetail detail = pokemonDetail(1, "bulbasaur", "image-1", "description-1");
        LocalPokemon expected = localPokemon(1, "bulbasaur", "image-1", "description-1",
                null, null, List.of());
        when(getPokemonDetailUseCase.get(1)).thenReturn(detail);
        when(localPokemonRepository.findById(1)).thenReturn(Optional.empty());
        when(localPokemonRepository.save(any(LocalPokemon.class))).thenReturn(expected);

        LocalPokemon result = localPokemonService.sync(1);

        assertThat(result).isEqualTo(expected);
        verify(localPokemonRepository).save(expected);
    }

    @Test
    void syncingTwiceSavesOncePerCallUsingTheSamePokemonId() {
        when(getPokemonDetailUseCase.get(1)).thenReturn(
                pokemonDetail(1, "bulbasaur", "image-1", "description-1"));
        when(localPokemonRepository.findById(1)).thenReturn(Optional.empty());
        when(localPokemonRepository.save(any(LocalPokemon.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LocalPokemon first = localPokemonService.sync(1);
        LocalPokemon second = localPokemonService.sync(1);

        assertThat(first.id()).isEqualTo(1);
        assertThat(second.id()).isEqualTo(1);
        ArgumentCaptor<LocalPokemon> savedPokemon = ArgumentCaptor.forClass(LocalPokemon.class);
        verify(localPokemonRepository, times(2)).save(savedPokemon.capture());
        assertThat(savedPokemon.getAllValues())
                .extracting(LocalPokemon::id)
                .containsExactly(1, 1);
    }

    @Test
    void syncPreservesLocalizedFieldsAndTagsWhileUpdatingProviderFields() {
        LocalPokemon existing = localPokemon(
                1, "old-name", "old-image", "old-description",
                "Nombre local", "Kanto", List.of("favorite", "starter"));
        when(getPokemonDetailUseCase.get(1))
                .thenReturn(pokemonDetail(1, "bulbasaur", "new-image", "new-description"));
        when(localPokemonRepository.findById(1)).thenReturn(Optional.of(existing));
        LocalPokemon saved = localPokemon(
                1, "bulbasaur", "new-image", "new-description",
                "Nombre local", "Kanto", List.of("favorite", "starter"));
        when(localPokemonRepository.save(any(LocalPokemon.class))).thenReturn(saved);

        LocalPokemon result = localPokemonService.sync(1);

        assertThat(result).isEqualTo(saved);
        verify(localPokemonRepository).save(saved);
    }

    @Test
    void returnsAnExistingLocalPokemonById() {
        LocalPokemon expected = localPokemon(
                25, "pikachu", "image-25", "description-25", "Pikachu", "Kanto", List.of());
        when(localPokemonRepository.findById(25)).thenReturn(Optional.of(expected));

        LocalPokemon result = localPokemonService.getById(25);

        assertThat(result).isSameAs(expected);
        verify(localPokemonRepository).findById(25);
    }

    @Test
    void throwsNotFoundWhenLocalPokemonDoesNotExist() {
        when(localPokemonRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> localPokemonService.getById(999))
                .isInstanceOf(LocalPokemonNotFoundException.class);
    }

    @Test
    void returnsAllLocalPokemonFromTheRepository() {
        List<LocalPokemon> expected = List.of(
                localPokemon(1, "bulbasaur", "image-1", "description-1", null, null, List.of()),
                localPokemon(4, "charmander", "image-4", "description-4", null, null, List.of()));
        when(localPokemonRepository.findAll()).thenReturn(expected);

        List<LocalPokemon> result = localPokemonService.list();

        assertThat(result).isSameAs(expected);
        verify(localPokemonRepository).findAll();
    }

    private PokemonDetail pokemonDetail(int id, String name, String imageUrl, String description) {
        return new PokemonDetail(id, name, imageUrl, List.of(new PokemonStat("hp", 45)),
                description, List.of(name));
    }

    private LocalPokemon localPokemon(
            int id,
            String name,
            String imageUrl,
            String description,
            String localizedName,
            String region,
            List<String> tags) {
        return new LocalPokemon(id, name, imageUrl, description, localizedName, region, tags);
    }
}
