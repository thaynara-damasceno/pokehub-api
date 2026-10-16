package com.thaynara.pokehub.infrastructure.config;

import com.thaynara.pokehub.application.port.out.LocalPokemonRepository;
import com.thaynara.pokehub.domain.model.LocalPokemon;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DemoPokemonSeederTest {

    private final LocalPokemonRepository repository = mock(LocalPokemonRepository.class);

    @Test
    void savesEveryPokemonThatIsNotStoredYet() {
        LocalPokemon pikachu = pokemon(25);
        LocalPokemon eevee = pokemon(133);
        when(repository.findById(25)).thenReturn(Optional.empty());
        when(repository.findById(133)).thenReturn(Optional.empty());

        DemoPokemonSeeder seeder = new DemoPokemonSeeder(repository, List.of(pikachu, eevee));
        seeder.run();

        verify(repository).save(pikachu);
        verify(repository).save(eevee);
    }

    @Test
    void doesNotOverwriteAPokemonThatAlreadyExists() {
        LocalPokemon existingPikachu = pokemon(25);
        LocalPokemon eevee = pokemon(133);
        when(repository.findById(25)).thenReturn(Optional.of(existingPikachu));
        when(repository.findById(133)).thenReturn(Optional.empty());

        DemoPokemonSeeder seeder = new DemoPokemonSeeder(repository, List.of(existingPikachu, eevee));
        seeder.run();

        verify(repository, never()).save(existingPikachu);
        verify(repository).save(eevee);
    }

    private LocalPokemon pokemon(int id) {
        return new LocalPokemon(id, "pokemon-" + id, "", "", "Localized " + id, "Kanto", List.of("demo"));
    }
}
