package com.thaynara.pokehub.infrastructure.web;

import com.thaynara.pokehub.domain.model.PokemonDetail;
import java.util.List;

public record PokemonDetailResponse(
        int id,
        String name,
        String imageUrl,
        List<PokemonStatResponse> stats,
        String description,
        List<String> evolutionChain
) {

    public static PokemonDetailResponse from(PokemonDetail pokemonDetail) {
        List<PokemonStatResponse> stats = pokemonDetail.stats().stream()
                .map(PokemonStatResponse::from)
                .toList();
        return new PokemonDetailResponse(
                pokemonDetail.id(),
                pokemonDetail.name(),
                pokemonDetail.imageUrl(),
                stats,
                pokemonDetail.description(),
                pokemonDetail.evolutionChain());
    }
}
