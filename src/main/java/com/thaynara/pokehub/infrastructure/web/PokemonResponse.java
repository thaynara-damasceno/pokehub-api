package com.thaynara.pokehub.infrastructure.web;

import java.util.List;

public record PokemonResponse(
        int id,
        String name,
        String spriteUrl,
        List<String> types,
        int weight,
        List<String> abilities
) {
}
