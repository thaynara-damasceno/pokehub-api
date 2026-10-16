package com.thaynara.pokehub.domain.model;

import java.util.List;

public record LocalPokemon(
        int id,
        String name,
        String imageUrl,
        String description,
        String localizedName,
        String region,
        List<String> tags
) {
    public LocalPokemon {
    }
}
