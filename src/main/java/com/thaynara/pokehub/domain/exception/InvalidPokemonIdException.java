package com.thaynara.pokehub.domain.exception;

public class InvalidPokemonIdException extends RuntimeException {

    public InvalidPokemonIdException(String message) {
        super(message);
    }
}
