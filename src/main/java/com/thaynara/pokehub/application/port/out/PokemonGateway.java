package com.thaynara.pokehub.application.port.out;

import com.thaynara.pokehub.domain.model.PokemonPage;

public interface PokemonGateway {

    PokemonPage findPage(int page, int size);
}
