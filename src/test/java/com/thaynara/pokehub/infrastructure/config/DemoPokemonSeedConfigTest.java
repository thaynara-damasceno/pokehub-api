package com.thaynara.pokehub.infrastructure.config;

import com.thaynara.pokehub.domain.model.LocalPokemon;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DemoPokemonSeedConfigTest {

    @Test
    void demoPokemonContainsFiveUniquePokemonWithLocalizedSeedDetails() {
        List<LocalPokemon> pokemon = DemoPokemonSeedConfig.demoPokemon();

        assertThat(pokemon).hasSize(5);
        assertThat(pokemon.stream().map(LocalPokemon::id).collect(Collectors.toSet()))
                .hasSize(5);
        assertThat(pokemon).allSatisfy(item -> {
            assertThat(item.localizedName()).isNotBlank();
            assertThat(item.region()).isNotBlank();
            assertThat(item.tags()).isNotEmpty();
        });
    }
}
