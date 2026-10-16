package com.thaynara.pokehub.domain.exception;

public class PokemonNotFoundException extends RuntimeException {

    public PokemonNotFoundException(int id) {
        super("Pokémon with id " + id + " was not found.");
    }
}
