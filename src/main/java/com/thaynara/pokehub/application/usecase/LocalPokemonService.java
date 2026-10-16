package com.thaynara.pokehub.application.usecase;

import com.thaynara.pokehub.application.port.in.GetLocalPokemonUseCase;
import com.thaynara.pokehub.application.port.in.ListLocalPokemonUseCase;
import com.thaynara.pokehub.application.port.in.SyncPokemonUseCase;
import com.thaynara.pokehub.application.port.out.LocalPokemonRepository;
import com.thaynara.pokehub.application.port.in.GetPokemonDetailUseCase;
import com.thaynara.pokehub.domain.model.LocalPokemon;
import java.util.List;

public class LocalPokemonService implements
        SyncPokemonUseCase, GetLocalPokemonUseCase, ListLocalPokemonUseCase {

    public LocalPokemonService(
            GetPokemonDetailUseCase getPokemonDetailUseCase,
            LocalPokemonRepository localPokemonRepository) {
    }

    @Override
    public LocalPokemon sync(int id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public LocalPokemon getById(int id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<LocalPokemon> list() {
        throw new UnsupportedOperationException();
    }
}
