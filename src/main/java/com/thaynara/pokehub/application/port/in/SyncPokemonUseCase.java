package com.thaynara.pokehub.application.port.in;

import com.thaynara.pokehub.domain.model.LocalPokemon;

public interface SyncPokemonUseCase {

    LocalPokemon sync(int id);
}
