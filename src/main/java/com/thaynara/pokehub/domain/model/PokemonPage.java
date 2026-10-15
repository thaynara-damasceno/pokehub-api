package com.thaynara.pokehub.domain.model;

import java.util.List;

public record PokemonPage(List<Pokemon> items, int page, int size, long totalElements) {

    public int totalPages() {
        if (size == 0) {
            return 0;
        }

        long pages = totalElements / size;
        if (totalElements % size != 0 && (totalElements >= 0) == (size >= 0)) {
            pages++;
        }
        return Math.toIntExact(pages);
    }
}
