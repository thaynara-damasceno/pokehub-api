package com.thaynara.pokehub.infrastructure.persistence;

import com.thaynara.pokehub.domain.model.LocalPokemon;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaLocalPokemonRepositoryAdapter.class)
class JpaLocalPokemonRepositoryAdapterTest {

    @Autowired
    private JpaLocalPokemonRepositoryAdapter adapter;

    @Test
    void savesAndFindsPokemonIncludingTagsInOrder() {
        LocalPokemon pokemon = localPokemon(
                1, "bulbasaur", "image", "description", "Bulbasaur", "Kanto",
                List.of("starter", "favorite"));

        adapter.save(pokemon);

        assertThat(adapter.findById(1)).contains(pokemon);
    }

    @Test
    void updatesExistingIdInsteadOfCreatingADuplicate() {
        adapter.save(localPokemon(
                1, "bulbasaur", "old-image", "old-description", null, null, List.of()));

        adapter.save(localPokemon(
                1, "bulbasaur", "new-image", "new-description", null, null, List.of()));

        assertThat(adapter.findAll()).hasSize(1);
        assertThat(adapter.findById(1)).get()
                .extracting(LocalPokemon::imageUrl)
                .isEqualTo("new-image");
    }

    @Test
    void returnsEmptyWhenIdIsUnknown() {
        assertThat(adapter.findById(999)).isEmpty();
    }

    @Test
    void returnsEverySavedPokemon() {
        LocalPokemon bulbasaur = localPokemon(
                1, "bulbasaur", "image-1", "description-1", null, null, List.of());
        LocalPokemon charmander = localPokemon(
                4, "charmander", "image-4", "description-4", null, null, List.of());
        adapter.save(bulbasaur);
        adapter.save(charmander);

        assertThat(adapter.findAll()).containsExactlyInAnyOrder(bulbasaur, charmander);
    }

    @Test
    void readsNullOptionalFieldsAndEmptyTags() {
        LocalPokemon pokemon = localPokemon(
                25, "pikachu", "image", "description", null, null, List.of());
        adapter.save(pokemon);

        Optional<LocalPokemon> result = adapter.findById(25);

        assertThat(result).contains(pokemon);
        assertThat(result.orElseThrow().localizedName()).isNull();
        assertThat(result.orElseThrow().region()).isNull();
        assertThat(result.orElseThrow().tags()).isEmpty();
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
