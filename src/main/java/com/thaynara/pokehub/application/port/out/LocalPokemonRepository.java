package com.thaynara.pokehub.application.port.out;

import com.thaynara.pokehub.domain.model.LocalPokemon;
import java.util.List;
import java.util.Optional;

public interface LocalPokemonRepository {

    Optional<LocalPokemon> findById(int id);

    List<LocalPokemon> findAll();

    LocalPokemon save(LocalPokemon pokemon);
}
