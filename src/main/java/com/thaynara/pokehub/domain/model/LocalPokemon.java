package com.thaynara.pokehub.domain.model;

import com.thaynara.pokehub.domain.exception.InvalidLocalPokemonException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        if (localizedName != null && localizedName.length() > 60) {
            throw new InvalidLocalPokemonException("Localized name must not exceed 60 characters.");
        }
        if (region != null && region.length() > 60) {
            throw new InvalidLocalPokemonException("Region must not exceed 60 characters.");
        }

        if (tags == null) {
            tags = List.of();
        }
        if (tags.size() > 10) {
            throw new InvalidLocalPokemonException("Tags must contain no more than 10 items.");
        }

        Set<String> uniqueTags = new HashSet<>();
        for (String tag : tags) {
            if (tag == null) {
                throw new InvalidLocalPokemonException("Tags must not contain null values.");
            }
            if (tag.length() > 30) {
                throw new InvalidLocalPokemonException("Each tag must not exceed 30 characters.");
            }
            if (!uniqueTags.add(tag)) {
                throw new InvalidLocalPokemonException("Tags must not contain duplicates.");
            }
        }
        tags = List.copyOf(tags);
    }
}
