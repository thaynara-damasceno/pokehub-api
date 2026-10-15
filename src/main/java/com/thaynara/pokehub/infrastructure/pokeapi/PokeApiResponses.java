package com.thaynara.pokehub.infrastructure.pokeapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

final class PokeApiResponses {

    private PokeApiResponses() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PokemonListResponse(long count, List<PokemonListItem> results) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PokemonListItem(String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PokemonDetail(
            int id,
            String name,
            int weight,
            PokemonSprites sprites,
            List<PokemonTypeEntry> types,
            List<PokemonAbilityEntry> abilities
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PokemonSprites(@JsonProperty("front_default") String frontDefault) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PokemonTypeEntry(int slot, PokemonType type) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PokemonType(String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PokemonAbilityEntry(
            @JsonProperty("is_hidden") boolean isHidden,
            int slot,
            PokemonAbility ability
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PokemonAbility(String name) {
    }
}
