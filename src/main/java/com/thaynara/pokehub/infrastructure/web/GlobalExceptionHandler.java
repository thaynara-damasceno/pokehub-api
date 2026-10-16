package com.thaynara.pokehub.infrastructure.web;

import com.thaynara.pokehub.domain.exception.InvalidPaginationException;
import com.thaynara.pokehub.domain.exception.InvalidLocalPokemonException;
import com.thaynara.pokehub.domain.exception.InvalidPokemonIdException;
import com.thaynara.pokehub.domain.exception.LocalPokemonNotFoundException;
import com.thaynara.pokehub.domain.exception.PokemonNotFoundException;
import com.thaynara.pokehub.domain.exception.PokemonProviderUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InvalidPaginationException.class)
    public ProblemDetail handleInvalidPagination(InvalidPaginationException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(InvalidLocalPokemonException.class)
    public ProblemDetail handleInvalidLocalPokemon(InvalidLocalPokemonException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(InvalidPokemonIdException.class)
    public ProblemDetail handleInvalidPokemonId(InvalidPokemonIdException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(PokemonNotFoundException.class)
    public ProblemDetail handlePokemonNotFound(PokemonNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(LocalPokemonNotFoundException.class)
    public ProblemDetail handleLocalPokemonNotFound(LocalPokemonNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(PokemonProviderUnavailableException.class)
    public ProblemDetail handlePokemonProviderUnavailable(
            PokemonProviderUnavailableException exception) {
        logger.error("PokeAPI provider is unavailable.", exception);
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_GATEWAY,
                "The Pokémon data provider is currently unavailable. Please try again later.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Invalid value for parameter '" + exception.getName() + "'.");
    }
}
