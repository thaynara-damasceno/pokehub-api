package com.thaynara.pokehub.domain.model;

import java.util.List;

public record PokemonDetail(
        int id,
        String name,
        String imageUrl,
        List<PokemonStat> stats,
        String description,
        List<String> evolutionChain
) {
}
