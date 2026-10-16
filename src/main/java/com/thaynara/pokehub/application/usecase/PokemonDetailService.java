package com.thaynara.pokehub.application.usecase;

import com.thaynara.pokehub.application.port.in.GetPokemonDetailUseCase;
import com.thaynara.pokehub.application.port.out.PokemonGateway;
import com.thaynara.pokehub.domain.model.PokemonDetail;

public class PokemonDetailService implements GetPokemonDetailUseCase {

    private final PokemonGateway pokemonGateway;

    public PokemonDetailService(PokemonGateway pokemonGateway) {
        this.pokemonGateway = pokemonGateway;
    }

    @Override
    public PokemonDetail get(int id) {
        throw new UnsupportedOperationException();
    }
}
