package com.thaynara.pokehub.infrastructure.pokeapi;

import com.thaynara.pokehub.application.port.out.PokemonGateway;
import com.thaynara.pokehub.domain.exception.PokemonProviderUnavailableException;
import com.thaynara.pokehub.domain.model.Pokemon;
import com.thaynara.pokehub.domain.model.PokemonPage;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClient;

public class PokeApiPokemonGateway implements PokemonGateway {

    private static final ObjectMapper JSON_MAPPER = JsonMapper.shared();

    private final RestClient restClient;

    public PokeApiPokemonGateway(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public PokemonPage findPage(int page, int size) {
        PokemonListResponse response = get("/pokemon?limit={size}&offset={offset}",
                PokemonListResponse.class, size, page * size);
        if (response == null) {
            throw new PokemonProviderUnavailableException("PokeAPI returned an empty Pokémon page.");
        }

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<Pokemon>> pokemonFutures = response.results().stream()
                    .map(item -> CompletableFuture.supplyAsync(
                            () -> fetchPokemon(item.name()), executor))
                    .toList();

            List<Pokemon> pokemon = pokemonFutures.stream()
                    .map(this::joinPokemon)
                    .toList();
            return new PokemonPage(pokemon, page, size, response.count());
        }
    }

    private Pokemon fetchPokemon(String name) {
        PokemonDetail detail = get("/pokemon/{name}", PokemonDetail.class, name);
        if (detail == null) {
            throw new PokemonProviderUnavailableException(
                    "PokeAPI returned no details for Pokémon " + name + ".");
        }

        List<String> types = detail.types().stream()
                .sorted((first, second) -> Integer.compare(first.slot(), second.slot()))
                .map(type -> type.type().name())
                .toList();
        List<String> abilities = detail.abilities().stream()
                .sorted((first, second) -> Integer.compare(first.slot(), second.slot()))
                .map(ability -> ability.ability().name())
                .toList();

        return new Pokemon(
                detail.id(),
                detail.name(),
                detail.sprites().frontDefault(),
                types,
                detail.weight(),
                abilities);
    }

    private <T> T get(String uri, Class<T> responseType, Object... uriVariables) {
        try {
            String responseBody = restClient.get()
                    .uri(uri, uriVariables)
                    .retrieve()
                    .body(String.class);
            return responseBody == null ? null : JSON_MAPPER.readValue(responseBody, responseType);
        } catch (RestClientException exception) {
            throw new PokemonProviderUnavailableException(
                    "PokeAPI request failed for " + uri + ".", exception);
        } catch (JacksonException exception) {
            throw new PokemonProviderUnavailableException(
                    "PokeAPI returned an invalid response for " + uri + ".", exception);
        }
    }

    private Pokemon joinPokemon(CompletableFuture<Pokemon> future) {
        try {
            return future.join();
        } catch (CompletionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof PokemonProviderUnavailableException providerException) {
                throw providerException;
            }
            throw new PokemonProviderUnavailableException(
                    "Failed to retrieve Pokémon details from PokeAPI.", cause);
        }
    }
}
