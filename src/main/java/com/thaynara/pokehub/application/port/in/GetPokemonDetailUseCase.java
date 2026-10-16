package com.thaynara.pokehub.application.port.in;

import com.thaynara.pokehub.domain.model.PokemonDetail;

public interface GetPokemonDetailUseCase {

    PokemonDetail get(int id);
}
