package com.thaynara.pokehub.infrastructure.config;

import com.thaynara.pokehub.application.port.out.LocalPokemonRepository;
import com.thaynara.pokehub.domain.model.LocalPokemon;
import java.util.List;
import org.springframework.boot.CommandLineRunner;

public class DemoPokemonSeeder implements CommandLineRunner {

    private final LocalPokemonRepository repository;
    private final List<LocalPokemon> pokemon;

    public DemoPokemonSeeder(LocalPokemonRepository repository, List<LocalPokemon> pokemon) {
        this.repository = repository;
        this.pokemon = pokemon;
    }

    @Override
    public void run(String... args) {
    }
}
