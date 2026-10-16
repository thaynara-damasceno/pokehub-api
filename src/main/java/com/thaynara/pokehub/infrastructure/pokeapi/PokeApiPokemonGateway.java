package com.thaynara.pokehub.infrastructure.pokeapi;

import com.thaynara.pokehub.application.port.out.PokemonGateway;
import com.thaynara.pokehub.domain.exception.PokemonNotFoundException;
import com.thaynara.pokehub.domain.exception.PokemonProviderUnavailableException;
import com.thaynara.pokehub.domain.model.Pokemon;
import com.thaynara.pokehub.domain.model.PokemonDetail;
import com.thaynara.pokehub.domain.model.PokemonPage;
import com.thaynara.pokehub.domain.model.PokemonStat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public class PokeApiPokemonGateway implements PokemonGateway {

    private final RestClient restClient;

    public PokeApiPokemonGateway(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public PokemonPage findPage(int page, int size) {
        PokeApiResponses.PokemonListResponse response = get(
                "/pokemon?limit={size}&offset={offset}",
                PokeApiResponses.PokemonListResponse.class, size, page * size);
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

    @Override
    public PokemonDetail findById(int id) {
        PokeApiResponses.PokemonDetail pokemon;
        try {
            pokemon = restClient.get()
                    .uri("/pokemon/{id}", id)
                    .retrieve()
                    .body(PokeApiResponses.PokemonDetail.class);
        } catch (HttpClientErrorException.NotFound exception) {
            throw new PokemonNotFoundException(id);
        } catch (RestClientException exception) {
            throw new PokemonProviderUnavailableException(
                    "PokeAPI request failed for Pokémon " + id + ".", exception);
        }

        if (pokemon == null || pokemon.species() == null || pokemon.species().name() == null) {
            throw new PokemonProviderUnavailableException(
                    "PokeAPI returned incomplete details for Pokémon " + id + ".");
        }

        PokeApiResponses.PokemonSpecies species = get(
                "/pokemon-species/{name}",
                PokeApiResponses.PokemonSpecies.class,
                pokemon.species().name());
        String chainId = extractEvolutionChainId(species, id);
        PokeApiResponses.PokemonEvolutionChain evolutionChain = get(
                "/evolution-chain/{chainId}",
                PokeApiResponses.PokemonEvolutionChain.class,
                chainId);

        List<PokemonStat> stats = pokemon.stats().stream()
                .map(stat -> new PokemonStat(stat.stat().name(), stat.baseStat()))
                .toList();
        String description = species.flavorTextEntries() == null
                ? ""
                : species.flavorTextEntries().stream()
                        .filter(entry -> entry.language() != null
                                && "en".equals(entry.language().name()))
                        .map(PokeApiResponses.PokemonFlavorTextEntry::flavorText)
                        .filter(text -> text != null)
                        .findFirst()
                        .map(text -> text.replaceAll("\\s+", " ").trim())
                        .orElse("");
        List<String> evolutionNames = new ArrayList<>();
        if (evolutionChain != null && evolutionChain.chain() != null) {
            appendEvolutionNames(evolutionChain.chain(), evolutionNames);
        }

        return new PokemonDetail(
                pokemon.id(),
                pokemon.name(),
                pokemon.sprites() == null ? null : pokemon.sprites().frontDefault(),
                stats,
                description,
                evolutionNames);
    }

    private String extractEvolutionChainId(PokeApiResponses.PokemonSpecies species, int pokemonId) {
        if (species == null || species.evolutionChain() == null
                || species.evolutionChain().url() == null) {
            throw new PokemonProviderUnavailableException(
                    "PokeAPI returned no evolution chain for Pokémon " + pokemonId + ".");
        }
        String[] segments = species.evolutionChain().url().split("/");
        String lastSegment = "";
        for (int index = segments.length - 1; index >= 0; index--) {
            if (!segments[index].isEmpty()) {
                lastSegment = segments[index];
                break;
            }
        }
        if (!lastSegment.matches("\\d+")) {
            throw new PokemonProviderUnavailableException(
                    "PokeAPI returned an invalid evolution chain URL for Pokémon " + pokemonId + ".");
        }
        return lastSegment;
    }

    private void appendEvolutionNames(
            PokeApiResponses.PokemonEvolutionNode node, List<String> evolutionNames) {
        evolutionNames.add(node.species().name());
        if (node.evolvesTo() != null) {
            node.evolvesTo().forEach(child -> appendEvolutionNames(child, evolutionNames));
        }
    }

    private Pokemon fetchPokemon(String name) {
        PokeApiResponses.PokemonDetail detail = get(
                "/pokemon/{name}", PokeApiResponses.PokemonDetail.class, name);
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
            return restClient.get()
                    .uri(uri, uriVariables)
                    .retrieve()
                    .body(responseType);
        } catch (RestClientException exception) {
            throw new PokemonProviderUnavailableException(
                    "PokeAPI request failed for " + uri + ".", exception);
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
