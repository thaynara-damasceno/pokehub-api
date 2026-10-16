package com.thaynara.pokehub.application.usecase;

import com.thaynara.pokehub.application.port.in.GetPokemonDetailUseCase;
import com.thaynara.pokehub.application.port.out.LocalPokemonRepository;
import com.thaynara.pokehub.domain.exception.InvalidLocalPokemonException;
import com.thaynara.pokehub.domain.exception.LocalPokemonNotFoundException;
import com.thaynara.pokehub.domain.model.LocalPokemon;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalPokemonServiceUpdateDeleteTest {

    @Mock
    private GetPokemonDetailUseCase getPokemonDetailUseCase;

    @Mock
    private LocalPokemonRepository localPokemonRepository;

    @InjectMocks
    private LocalPokemonService localPokemonService;

    @Test
    void updatesOnlyLocalizedFieldsAndTags() {
        LocalPokemon existing = existingPokemon();
        LocalPokemon expected = new LocalPokemon(
                25, "pikachu", "image-25", "description-25",
                "Pikachu local", "Kanto", List.of("favorite"));
        when(localPokemonRepository.findById(25)).thenReturn(Optional.of(existing));
        when(localPokemonRepository.save(any(LocalPokemon.class))).thenReturn(expected);

        LocalPokemon result = localPokemonService.update(
                25, "Pikachu local", "Kanto", List.of("favorite"));

        assertThat(result).isEqualTo(expected);
        verify(localPokemonRepository).save(expected);
        verifyNoInteractions(getPokemonDetailUseCase);
    }

    @Test
    void updateWithNullTagsStoresAnEmptyList() {
        when(localPokemonRepository.findById(25)).thenReturn(Optional.of(existingPokemon()));
        when(localPokemonRepository.save(any(LocalPokemon.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LocalPokemon result = localPokemonService.update(25, null, null, null);

        assertThat(result.tags()).isEmpty();
        assertThat(result.localizedName()).isNull();
        assertThat(result.region()).isNull();
    }

    @Test
    void updateOfUnknownIdThrowsNotFoundWithoutSaving() {
        when(localPokemonRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> localPokemonService.update(999, null, null, List.of()))
                .isInstanceOf(LocalPokemonNotFoundException.class);

        verify(localPokemonRepository, never()).save(any(LocalPokemon.class));
    }

    @Test
    void updateWithInvalidLocalizedNameThrowsWithoutSaving() {
        when(localPokemonRepository.findById(25)).thenReturn(Optional.of(existingPokemon()));

        assertThatThrownBy(() -> localPokemonService.update(
                25, "n".repeat(61), null, List.of()))
                .isInstanceOf(InvalidLocalPokemonException.class);

        verify(localPokemonRepository, never()).save(any(LocalPokemon.class));
    }

    @Test
    void updateWithTooManyTagsThrowsWithoutSaving() {
        when(localPokemonRepository.findById(25)).thenReturn(Optional.of(existingPokemon()));
        List<String> tags = List.of(
                "tag-1", "tag-2", "tag-3", "tag-4", "tag-5", "tag-6",
                "tag-7", "tag-8", "tag-9", "tag-10", "tag-11");

        assertThatThrownBy(() -> localPokemonService.update(25, null, null, tags))
                .isInstanceOf(InvalidLocalPokemonException.class);

        verify(localPokemonRepository, never()).save(any(LocalPokemon.class));
    }

    @Test
    void deleteOfExistingIdCallsRepositoryOnce() {
        when(localPokemonRepository.findById(25)).thenReturn(Optional.of(existingPokemon()));

        localPokemonService.delete(25);

        verify(localPokemonRepository).deleteById(25);
    }

    @Test
    void deleteOfUnknownIdThrowsNotFoundWithoutDeleting() {
        when(localPokemonRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> localPokemonService.delete(999))
                .isInstanceOf(LocalPokemonNotFoundException.class);

        verify(localPokemonRepository, never()).deleteById(999);
    }

    private LocalPokemon existingPokemon() {
        return new LocalPokemon(
                25, "pikachu", "image-25", "description-25",
                "Pikachu", "Kanto", List.of("favorite", "starter"));
    }
}
