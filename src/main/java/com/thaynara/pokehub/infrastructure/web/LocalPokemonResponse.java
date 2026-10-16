package com.thaynara.pokehub.infrastructure.web;

import com.thaynara.pokehub.domain.model.LocalPokemon;
import java.util.List;

public record LocalPokemonResponse(
        int id,
        String name,
        String imageUrl,
        String description,
        String localizedName,
        String region,
        List<String> tags
) {

    public static LocalPokemonResponse from(LocalPokemon pokemon) {
        return new LocalPokemonResponse(
                pokemon.id(),
                pokemon.name(),
                pokemon.imageUrl(),
                pokemon.description(),
                pokemon.localizedName(),
                pokemon.region(),
                pokemon.tags());
    }
}
