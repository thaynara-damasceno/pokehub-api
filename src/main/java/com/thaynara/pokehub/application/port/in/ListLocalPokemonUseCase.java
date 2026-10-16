package com.thaynara.pokehub.application.port.in;

import com.thaynara.pokehub.domain.model.LocalPokemon;
import java.util.List;

public interface ListLocalPokemonUseCase {

    List<LocalPokemon> list();
}
