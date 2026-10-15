package com.thaynara.pokehub.infrastructure.web;

import com.thaynara.pokehub.domain.model.PokemonPage;
import java.util.List;

public record PokemonPageResponse(
        List<PokemonResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static PokemonPageResponse from(PokemonPage pokemonPage) {
        List<PokemonResponse> items = pokemonPage.items().stream()
                .map(PokemonResponse::from)
                .toList();
        return new PokemonPageResponse(
                items,
                pokemonPage.page(),
                pokemonPage.size(),
                pokemonPage.totalElements(),
                pokemonPage.totalPages());
    }
}
