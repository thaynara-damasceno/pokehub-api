package com.thaynara.pokehub.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataLocalPokemonRepository
        extends JpaRepository<LocalPokemonEntity, Integer> {
}
