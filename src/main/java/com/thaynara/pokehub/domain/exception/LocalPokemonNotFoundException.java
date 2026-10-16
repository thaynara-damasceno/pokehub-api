package com.thaynara.pokehub.domain.exception;

public class LocalPokemonNotFoundException extends RuntimeException {

    public LocalPokemonNotFoundException(int id) {
        super("Local Pokémon with id " + id + " was not found.");
    }
}
