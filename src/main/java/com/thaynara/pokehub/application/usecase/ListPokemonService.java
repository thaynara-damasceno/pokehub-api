package com.thaynara.pokehub.application.usecase;

import com.thaynara.pokehub.application.port.in.ListPokemonUseCase;
import com.thaynara.pokehub.application.port.out.PokemonGateway;
import com.thaynara.pokehub.domain.exception.InvalidPaginationException;
import com.thaynara.pokehub.domain.model.PokemonPage;

public class ListPokemonService implements ListPokemonUseCase {

    static final int MAX_PAGE_SIZE = 50;

    private final PokemonGateway pokemonGateway;

    public ListPokemonService(PokemonGateway pokemonGateway) {
        this.pokemonGateway = pokemonGateway;
    }

    @Override
    public PokemonPage list(int page, int size) {
        if (page < 0) {
            throw new InvalidPaginationException("Page must be greater than or equal to 0.");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidPaginationException(
                    "Page size must be between 1 and " + MAX_PAGE_SIZE + ".");
        }

        return pokemonGateway.findPage(page, size);
    }
}
