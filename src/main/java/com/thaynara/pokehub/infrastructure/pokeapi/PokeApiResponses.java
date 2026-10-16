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
            Integer weight,
            PokemonSprites sprites,
            List<PokemonTypeEntry> types,
            List<PokemonAbilityEntry> abilities,
            List<PokemonStatEntry> stats,
            PokemonSpeciesReference species
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PokemonSprites(@JsonProperty("front_default") String frontDefault) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PokemonStatEntry(
            @JsonProperty("base_stat") int baseStat,
            PokemonStatReference stat
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PokemonStatReference(String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PokemonSpeciesReference(String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PokemonSpecies(
            @JsonProperty("flavor_text_entries") List<PokemonFlavorTextEntry> flavorTextEntries,
            @JsonProperty("evolution_chain") PokemonEvolutionChainReference evolutionChain
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PokemonFlavorTextEntry(
            @JsonProperty("flavor_text") String flavorText,
            PokemonLanguageReference language
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PokemonLanguageReference(String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PokemonEvolutionChainReference(String url) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PokemonEvolutionChain(PokemonEvolutionNode chain) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PokemonEvolutionNode(
            PokemonSpeciesReference species,
            @JsonProperty("evolves_to") List<PokemonEvolutionNode> evolvesTo
    ) {
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
