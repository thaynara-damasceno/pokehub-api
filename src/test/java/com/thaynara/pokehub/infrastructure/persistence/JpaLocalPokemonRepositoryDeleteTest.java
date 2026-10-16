package com.thaynara.pokehub.infrastructure.persistence;

import com.thaynara.pokehub.domain.model.LocalPokemon;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaLocalPokemonRepositoryAdapter.class)
class JpaLocalPokemonRepositoryDeleteTest {

    @Autowired
    private JpaLocalPokemonRepositoryAdapter adapter;

    @Test
    void deleteByIdRemovesTheRecord() {
        adapter.save(new LocalPokemon(
                25, "pikachu", "image", "description", null, null, List.of()));

        adapter.deleteById(25);

        assertThat(adapter.findById(25)).isEmpty();
        assertThat(adapter.findAll()).noneMatch(pokemon -> pokemon.id() == 25);
    }

    @Test
    void deleteByIdOfUnknownIdDoesNotThrow() {
        adapter.deleteById(999);
    }
}
