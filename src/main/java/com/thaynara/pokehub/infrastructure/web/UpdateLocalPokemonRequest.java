package com.thaynara.pokehub.infrastructure.web;

import java.util.List;

public record UpdateLocalPokemonRequest(String localizedName, String region, List<String> tags) {
}
