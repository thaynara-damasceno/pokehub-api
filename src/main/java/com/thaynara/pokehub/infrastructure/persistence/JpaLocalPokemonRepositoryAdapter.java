package com.thaynara.pokehub.infrastructure.persistence;

import com.thaynara.pokehub.application.port.out.LocalPokemonRepository;
import com.thaynara.pokehub.domain.model.LocalPokemon;
import java.util.ArrayList;
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
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<LocalPokemon> findAll() {
        return repository.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public LocalPokemon save(LocalPokemon pokemon) {
        LocalPokemonEntity entity = toEntity(pokemon);
        return toDomain(repository.save(entity));
    }

    @Override
    public void deleteById(int id) {
        throw new UnsupportedOperationException();
    }

    private LocalPokemon toDomain(LocalPokemonEntity entity) {
        return new LocalPokemon(
                entity.getId(),
                entity.getName(),
                entity.getImageUrl(),
                entity.getDescription(),
                entity.getLocalizedName(),
                entity.getRegion(),
                entity.getTags());
    }

    private LocalPokemonEntity toEntity(LocalPokemon pokemon) {
        LocalPokemonEntity entity = new LocalPokemonEntity();
        entity.setId(pokemon.id());
        entity.setName(pokemon.name());
        entity.setImageUrl(pokemon.imageUrl());
        entity.setDescription(pokemon.description());
        entity.setLocalizedName(pokemon.localizedName());
        entity.setRegion(pokemon.region());
        entity.setTags(new ArrayList<>(pokemon.tags()));
        return entity;
    }
}
