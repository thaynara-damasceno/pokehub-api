package com.thaynara.pokehub.infrastructure.web;

import com.thaynara.pokehub.domain.model.PokemonStat;

public record PokemonStatResponse(String name, int value) {

    public static PokemonStatResponse from(PokemonStat pokemonStat) {
        return new PokemonStatResponse(pokemonStat.name(), pokemonStat.value());
    }
}
