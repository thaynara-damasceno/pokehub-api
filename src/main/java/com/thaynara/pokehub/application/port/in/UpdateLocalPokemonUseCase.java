package com.thaynara.pokehub.application.port.in;

import com.thaynara.pokehub.domain.model.LocalPokemon;
import java.util.List;

public interface UpdateLocalPokemonUseCase {

    LocalPokemon update(int id, String localizedName, String region, List<String> tags);
}
