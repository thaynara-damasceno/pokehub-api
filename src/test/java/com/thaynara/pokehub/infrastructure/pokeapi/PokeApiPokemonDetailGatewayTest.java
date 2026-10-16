package com.thaynara.pokehub.infrastructure.pokeapi;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.thaynara.pokehub.domain.exception.PokemonNotFoundException;
import com.thaynara.pokehub.domain.exception.PokemonProviderUnavailableException;
import com.thaynara.pokehub.domain.model.PokemonDetail;
import com.thaynara.pokehub.domain.model.PokemonStat;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.client.RestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PokeApiPokemonDetailGatewayTest {

    @RegisterExtension
    private static final WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Test
    void mapsPokemonSpeciesAndEvolutionResponsesAndUsesTheirExpectedPaths() throws IOException {
        stubJson("/api/v2/pokemon/1", fixture("pokemon-bulbasaur.json"));
        stubJson("/api/v2/pokemon-species/bulbasaur", fixture("species-bulbasaur.json"));
        stubJson("/api/v2/evolution-chain/7", fixture("evolution-chain-7.json"));

        PokemonDetail detail = gateway().findById(1);

        assertThat(detail).isEqualTo(new PokemonDetail(
                1,
                "bulbasaur",
                "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png",
                List.of(new PokemonStat("hp", 45), new PokemonStat("attack", 49)),
                "A strange seed was planted on its back at birth. The plant sprouts and grows with this Pokémon.",
                List.of("bulbasaur", "ivysaur", "venusaur")));
        wireMock.verify(getRequestedFor(urlPathEqualTo("/api/v2/pokemon/1")));
        wireMock.verify(getRequestedFor(urlPathEqualTo("/api/v2/pokemon-species/bulbasaur")));
        wireMock.verify(getRequestedFor(urlPathEqualTo("/api/v2/evolution-chain/7")));
    }

    @Test
    void flattensBranchingEvolutionChainsDepthFirst() {
        stubJson("/api/v2/pokemon/133", """
                {"id":133,"name":"eevee","sprites":{"front_default":"image"},
                 "stats":[],"species":{"name":"eevee"}}
                """);
        stubJson("/api/v2/pokemon-species/eevee", """
                {"flavor_text_entries":[{"flavor_text":"Description","language":{"name":"en"}}],
                 "evolution_chain":{"url":"https://pokeapi.co/api/v2/evolution-chain/133/"}}
                """);
        stubJson("/api/v2/evolution-chain/133", """
                {"chain":{"species":{"name":"eevee"},"evolves_to":[
                  {"species":{"name":"vaporeon"},"evolves_to":[]},
                  {"species":{"name":"jolteon"},"evolves_to":[]}]}}
                """);

        PokemonDetail detail = gateway().findById(133);

        assertThat(detail.evolutionChain()).containsExactly("eevee", "vaporeon", "jolteon");
    }

    @Test
    void throwsPokemonNotFoundWhenPokemonEndpointReturnsNotFound() {
        stubStatus("/api/v2/pokemon/999", 404);

        assertThatThrownBy(() -> gateway().findById(999))
                .isInstanceOf(PokemonNotFoundException.class)
                .hasMessage("Pokémon with id 999 was not found.");
    }

    @Test
    void throwsProviderUnavailableWhenPokemonEndpointReturnsServerError() {
        stubStatus("/api/v2/pokemon/1", 500);

        assertThatThrownBy(() -> gateway().findById(1))
                .isInstanceOf(PokemonProviderUnavailableException.class);
    }

    @Test
    void throwsProviderUnavailableWhenSpeciesEndpointReturnsNotFoundOrServerError()
            throws IOException {
        stubJson("/api/v2/pokemon/1", fixture("pokemon-bulbasaur.json"));
        stubStatus("/api/v2/pokemon-species/bulbasaur", 404);

        assertThatThrownBy(() -> gateway().findById(1))
                .isInstanceOf(PokemonProviderUnavailableException.class);

        wireMock.resetAll();
        stubJson("/api/v2/pokemon/1", fixture("pokemon-bulbasaur.json"));
        stubStatus("/api/v2/pokemon-species/bulbasaur", 500);

        assertThatThrownBy(() -> gateway().findById(1))
                .isInstanceOf(PokemonProviderUnavailableException.class);
    }

    @Test
    void throwsProviderUnavailableWhenEvolutionEndpointReturnsServerError() throws IOException {
        stubJson("/api/v2/pokemon/1", fixture("pokemon-bulbasaur.json"));
        stubJson("/api/v2/pokemon-species/bulbasaur", fixture("species-bulbasaur.json"));
        stubStatus("/api/v2/evolution-chain/7", 500);

        assertThatThrownBy(() -> gateway().findById(1))
                .isInstanceOf(PokemonProviderUnavailableException.class);
    }

    @Test
    void throwsProviderUnavailableWhenEvolutionChainUrlHasNoNumericId() throws IOException {
        stubJson("/api/v2/pokemon/1", fixture("pokemon-bulbasaur.json"));
        stubJson("/api/v2/pokemon-species/bulbasaur", """
                {"flavor_text_entries":[],"evolution_chain":{"url":"https://pokeapi.co/api/v2/evolution-chain/not-a-number/"}}
                """);

        assertThatThrownBy(() -> gateway().findById(1))
                .isInstanceOf(PokemonProviderUnavailableException.class);
    }

    private PokeApiPokemonGateway gateway() {
        return new PokeApiPokemonGateway(
                RestClient.builder().baseUrl(wireMock.baseUrl() + "/api/v2").build());
    }

    private void stubJson(String path, String body) {
        wireMock.stubFor(get(urlPathEqualTo(path))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));
    }

    private void stubStatus(String path, int status) {
        wireMock.stubFor(get(urlPathEqualTo(path))
                .willReturn(aResponse().withStatus(status)));
    }

    private String fixture(String name) throws IOException {
        try (InputStream stream = getClass().getResourceAsStream("/pokeapi/" + name)) {
            if (stream == null) {
                throw new IOException("Missing PokeAPI fixture: " + name);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
