package com.thaynara.pokehub.domain.model;

import com.thaynara.pokehub.domain.exception.InvalidLocalPokemonException;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalPokemonTest {

    @Test
    void acceptsAValidPokemonAndKeepsItsValues() {
        List<String> tags = List.of("starter", "favorite");
        LocalPokemon pokemon = new LocalPokemon(
                1, "bulbasaur", "image", "description", "Bulbasaur", "Kanto", tags);

        assertThat(pokemon.tags()).isEqualTo(tags);
        assertThat(pokemon.localizedName()).isEqualTo("Bulbasaur");
        assertThat(pokemon.region()).isEqualTo("Kanto");
    }

    @Test
    void acceptsNullLocalizedNameAndRegion() {
        LocalPokemon pokemon = new LocalPokemon(
                1, "bulbasaur", "image", "description", null, null, List.of());

        assertThat(pokemon.localizedName()).isNull();
        assertThat(pokemon.region()).isNull();
    }

    @Test
    void acceptsLocalizedNameAndRegionAtTheirMaximumLength() {
        new LocalPokemon(1, "bulbasaur", "image", "description",
                "n".repeat(60), "r".repeat(60), List.of());
    }

    @Test
    void rejectsLocalizedNameLongerThanSixtyCharacters() {
        assertThatThrownBy(() -> new LocalPokemon(
                1, "bulbasaur", "image", "description", "n".repeat(61), null, List.of()))
                .isInstanceOf(InvalidLocalPokemonException.class);
    }

    @Test
    void rejectsRegionLongerThanSixtyCharacters() {
        assertThatThrownBy(() -> new LocalPokemon(
                1, "bulbasaur", "image", "description", null, "r".repeat(61), List.of()))
                .isInstanceOf(InvalidLocalPokemonException.class);
    }

    @Test
    void acceptsTenDistinctTagsOfUpToThirtyCharacters() {
        List<String> tags = List.of(
                "a".repeat(30), "tag-2", "tag-3", "tag-4", "tag-5",
                "tag-6", "tag-7", "tag-8", "tag-9", "tag-10");

        assertThat(new LocalPokemon(
                1, "bulbasaur", "image", "description", null, null, tags).tags())
                .containsExactlyElementsOf(tags);
    }

    @Test
    void treatsNullTagsAsAnEmptyList() {
        assertThat(new LocalPokemon(
                1, "bulbasaur", "image", "description", null, null, null).tags())
                .isEmpty();
    }

    @Test
    void rejectsMoreThanTenTags() {
        List<String> tags = List.of(
                "tag-1", "tag-2", "tag-3", "tag-4", "tag-5", "tag-6",
                "tag-7", "tag-8", "tag-9", "tag-10", "tag-11");

        assertThatThrownBy(() -> new LocalPokemon(
                1, "bulbasaur", "image", "description", null, null, tags))
                .isInstanceOf(InvalidLocalPokemonException.class);
    }

    @Test
    void rejectsATagLongerThanThirtyCharacters() {
        assertThatThrownBy(() -> new LocalPokemon(
                1, "bulbasaur", "image", "description", null, null, List.of("t".repeat(31))))
                .isInstanceOf(InvalidLocalPokemonException.class);
    }

    @Test
    void rejectsDuplicateTags() {
        assertThatThrownBy(() -> new LocalPokemon(
                1, "bulbasaur", "image", "description", null, null, List.of("starter", "starter")))
                .isInstanceOf(InvalidLocalPokemonException.class);
    }
}
