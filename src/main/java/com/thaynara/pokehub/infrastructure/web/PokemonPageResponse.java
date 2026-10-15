package com.thaynara.pokehub.infrastructure.web;

import java.util.List;

public record PokemonPageResponse(
        List<PokemonResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
