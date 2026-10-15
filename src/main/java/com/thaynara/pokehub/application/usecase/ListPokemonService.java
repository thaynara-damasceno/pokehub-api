package com.thaynara.pokehub.application.usecase;

import com.thaynara.pokehub.application.port.in.ListPokemonUseCase;
import com.thaynara.pokehub.application.port.out.PokemonGateway;
import com.thaynara.pokehub.domain.model.PokemonPage;

public class ListPokemonService implements ListPokemonUseCase {

    static final int MAX_PAGE_SIZE = 50;

    private final PokemonGateway pokemonGateway;

    public ListPokemonService(PokemonGateway pokemonGateway) {
        this.pokemonGateway = pokemonGateway;
    }

    @Override
    public PokemonPage list(int page, int size) {
        throw new UnsupportedOperationException();
    }
}
