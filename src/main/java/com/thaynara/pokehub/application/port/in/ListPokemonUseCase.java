package com.thaynara.pokehub.application.port.in;

import com.thaynara.pokehub.domain.model.PokemonPage;

public interface ListPokemonUseCase {

    PokemonPage list(int page, int size);
}
