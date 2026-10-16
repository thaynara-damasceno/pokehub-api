package com.thaynara.pokehub.infrastructure.persistence;

import com.thaynara.pokehub.application.port.out.LocalPokemonRepository;
import com.thaynara.pokehub.domain.model.LocalPokemon;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaLocalPokemonRepositoryAdapter implements LocalPokemonRepository {

    private final SpringDataLocalPokemonRepository repository;

    public JpaLocalPokemonRepositoryAdapter(SpringDataLocalPokemonRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<LocalPokemon> findById(int id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<LocalPokemon> findAll() {
        throw new UnsupportedOperationException();
    }

    @Override
    public LocalPokemon save(LocalPokemon pokemon) {
        throw new UnsupportedOperationException();
    }
}
