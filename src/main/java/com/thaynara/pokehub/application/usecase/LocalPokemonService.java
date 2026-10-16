package com.thaynara.pokehub.application.usecase;

import com.thaynara.pokehub.application.port.in.GetLocalPokemonUseCase;
import com.thaynara.pokehub.application.port.in.GetPokemonDetailUseCase;
import com.thaynara.pokehub.application.port.in.ListLocalPokemonUseCase;
import com.thaynara.pokehub.application.port.in.SyncPokemonUseCase;
import com.thaynara.pokehub.application.port.in.UpdateLocalPokemonUseCase;
import com.thaynara.pokehub.application.port.in.DeleteLocalPokemonUseCase;
import com.thaynara.pokehub.application.port.out.LocalPokemonRepository;
import com.thaynara.pokehub.domain.exception.LocalPokemonNotFoundException;
import com.thaynara.pokehub.domain.model.LocalPokemon;
import com.thaynara.pokehub.domain.model.PokemonDetail;
import java.util.List;
import java.util.Optional;

public class LocalPokemonService implements
        SyncPokemonUseCase, GetLocalPokemonUseCase, ListLocalPokemonUseCase,
        UpdateLocalPokemonUseCase, DeleteLocalPokemonUseCase {

    private final GetPokemonDetailUseCase getPokemonDetailUseCase;
    private final LocalPokemonRepository localPokemonRepository;

    public LocalPokemonService(
            GetPokemonDetailUseCase getPokemonDetailUseCase,
            LocalPokemonRepository localPokemonRepository) {
        this.getPokemonDetailUseCase = getPokemonDetailUseCase;
        this.localPokemonRepository = localPokemonRepository;
    }

    @Override
    public LocalPokemon sync(int id) {
        PokemonDetail detail = getPokemonDetailUseCase.get(id);
        Optional<LocalPokemon> existing = localPokemonRepository.findById(id);
        LocalPokemon updated = existing
                .map(pokemon -> new LocalPokemon(
                        id,
                        detail.name(),
                        detail.imageUrl(),
                        detail.description(),
                        pokemon.localizedName(),
                        pokemon.region(),
                        pokemon.tags()))
                .orElseGet(() -> new LocalPokemon(
                        id,
                        detail.name(),
                        detail.imageUrl(),
                        detail.description(),
                        null,
                        null,
                        List.of()));
        return localPokemonRepository.save(updated);
    }

    @Override
    public LocalPokemon getById(int id) {
        return localPokemonRepository.findById(id)
                .orElseThrow(() -> new LocalPokemonNotFoundException(id));
    }

    @Override
    public List<LocalPokemon> list() {
        return localPokemonRepository.findAll();
    }

    @Override
    public LocalPokemon update(int id, String localizedName, String region, List<String> tags) {
        LocalPokemon existing = localPokemonRepository.findById(id)
                .orElseThrow(() -> new LocalPokemonNotFoundException(id));
        LocalPokemon updated = new LocalPokemon(
                existing.id(),
                existing.name(),
                existing.imageUrl(),
                existing.description(),
                localizedName,
                region,
                tags);
        return localPokemonRepository.save(updated);
    }

    @Override
    public void delete(int id) {
        localPokemonRepository.findById(id)
                .orElseThrow(() -> new LocalPokemonNotFoundException(id));
        localPokemonRepository.deleteById(id);
    }
}
