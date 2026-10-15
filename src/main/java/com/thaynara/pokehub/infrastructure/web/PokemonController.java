package com.thaynara.pokehub.infrastructure.web;

import com.thaynara.pokehub.application.port.in.ListPokemonUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pokemon")
public class PokemonController {

    private final ListPokemonUseCase listPokemonUseCase;

    public PokemonController(ListPokemonUseCase listPokemonUseCase) {
        this.listPokemonUseCase = listPokemonUseCase;
    }

    @GetMapping
    public PokemonPageResponse list(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        return PokemonPageResponse.from(listPokemonUseCase.list(page, size));
    }
}
