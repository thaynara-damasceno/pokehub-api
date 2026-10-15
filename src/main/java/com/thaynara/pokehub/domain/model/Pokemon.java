package com.thaynara.pokehub.domain.model;

import java.util.List;

public record Pokemon(
        int id,
        String name,
        String spriteUrl,
        List<String> types,
        int weight,
        List<String> abilities
) {
}
