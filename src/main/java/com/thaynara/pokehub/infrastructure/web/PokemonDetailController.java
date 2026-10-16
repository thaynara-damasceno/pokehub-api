package com.thaynara.pokehub.infrastructure.web;

import com.thaynara.pokehub.application.port.in.GetPokemonDetailUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pokemon")
public class PokemonDetailController {

    private final GetPokemonDetailUseCase getPokemonDetailUseCase;

    public PokemonDetailController(GetPokemonDetailUseCase getPokemonDetailUseCase) {
        this.getPokemonDetailUseCase = getPokemonDetailUseCase;
    }

    @GetMapping("/{id}")
    public PokemonDetailResponse detail(@PathVariable int id) {
        throw new UnsupportedOperationException();
    }
}
