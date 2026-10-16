package com.thaynara.pokehub.infrastructure.web;

import java.util.List;

public record PokemonDetailResponse(
        int id,
        String name,
        String imageUrl,
        List<PokemonStatResponse> stats,
        String description,
        List<String> evolutionChain
) {
}
