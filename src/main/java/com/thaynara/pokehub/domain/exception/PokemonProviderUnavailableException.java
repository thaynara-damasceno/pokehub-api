package com.thaynara.pokehub.domain.exception;

public class PokemonProviderUnavailableException extends RuntimeException {

    public PokemonProviderUnavailableException(String message) {
        super(message);
    }

    public PokemonProviderUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
