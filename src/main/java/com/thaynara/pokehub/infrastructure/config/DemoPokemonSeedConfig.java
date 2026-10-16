package com.thaynara.pokehub.infrastructure.config;

import com.thaynara.pokehub.application.port.out.LocalPokemonRepository;
import com.thaynara.pokehub.domain.model.LocalPokemon;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class DemoPokemonSeedConfig {

    @Bean
    @Profile("!test")
    @ConditionalOnProperty(name = "pokehub.seed.enabled", havingValue = "true")
    DemoPokemonSeeder demoPokemonSeeder(LocalPokemonRepository repository) {
        return new DemoPokemonSeeder(repository, demoPokemon());
    }

    static List<LocalPokemon> demoPokemon() {
        return List.of();
    }
}
