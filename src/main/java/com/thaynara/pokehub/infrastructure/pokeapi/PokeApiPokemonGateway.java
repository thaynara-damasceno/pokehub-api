package com.thaynara.pokehub.infrastructure.pokeapi;

import com.thaynara.pokehub.application.port.out.PokemonGateway;
import com.thaynara.pokehub.domain.model.PokemonPage;
import org.springframework.web.client.RestClient;

public class PokeApiPokemonGateway implements PokemonGateway {

    private final RestClient restClient;

    public PokeApiPokemonGateway(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public PokemonPage findPage(int page, int size) {
        throw new UnsupportedOperationException();
    }
}
