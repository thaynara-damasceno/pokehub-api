package com.thaynara.pokehub.infrastructure.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateLocalPokemonRequest(
        @Size(max = 60) String localizedName,
        @Size(max = 60) String region,
        @Size(max = 10) List<@NotBlank @Size(max = 30) String> tags) {
}
