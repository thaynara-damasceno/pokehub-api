package com.thaynara.pokehub.application.port.out;

import com.thaynara.pokehub.domain.model.PokemonPage;
import com.thaynara.pokehub.domain.model.PokemonDetail;

public interface PokemonGateway {

    PokemonPage findPage(int page, int size);

    PokemonDetail findById(int id);
}
