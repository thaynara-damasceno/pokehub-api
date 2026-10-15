package com.thaynara.pokehub.infrastructure.web;

import com.thaynara.pokehub.domain.model.Pokemon;
import java.util.List;

public record PokemonResponse(
        int id,
        String name,
        String spriteUrl,
        List<String> types,
        int weight,
        List<String> abilities
) {

    public static PokemonResponse from(Pokemon pokemon) {
        return new PokemonResponse(
                pokemon.id(),
                pokemon.name(),
                pokemon.spriteUrl(),
                pokemon.types(),
                pokemon.weight(),
                pokemon.abilities());
    }
}
