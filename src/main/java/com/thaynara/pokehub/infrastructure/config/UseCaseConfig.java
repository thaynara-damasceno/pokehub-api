package com.thaynara.pokehub.infrastructure.config;

import com.thaynara.pokehub.application.port.in.ListPokemonUseCase;
import com.thaynara.pokehub.application.port.out.PokemonGateway;
import com.thaynara.pokehub.application.usecase.ListPokemonService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    ListPokemonUseCase listPokemonUseCase(PokemonGateway pokemonGateway) {
        return new ListPokemonService(pokemonGateway);
    }
}
