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
        return List.of(
                new LocalPokemon(
                        25,
                        "pikachu",
                        "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/25.png",
                        "Electric mouse Pokémon that stores power in its cheeks and releases it in bursts.",
                        "ピカチュウ",
                        "Kanto",
                        List.of("electric", "mascot")),
                new LocalPokemon(
                        1,
                        "bulbasaur",
                        "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png",
                        "Seed Pokémon that carries a plant bulb on its back, which grows as it evolves.",
                        "フシギダネ",
                        "Kanto",
                        List.of("grass", "poison", "starter")),
                new LocalPokemon(
                        4,
                        "charmander",
                        "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/4.png",
                        "Lizard Pokémon with a flame burning at the tip of its tail.",
                        "ヒトカゲ",
                        "Kanto",
                        List.of("fire", "starter")),
                new LocalPokemon(
                        7,
                        "squirtle",
                        "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/7.png",
                        "Tiny turtle Pokémon that withdraws into its shell and sprays water.",
                        "ゼニガメ",
                        "Kanto",
                        List.of("water", "starter")),
                new LocalPokemon(
                        133,
                        "eevee",
                        "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/133.png",
                        "Evolution Pokémon with an unstable genetic makeup that can evolve in several ways.",
                        "イーブイ",
                        "Kanto",
                        List.of("normal", "branching-evolution")));
    }
}
