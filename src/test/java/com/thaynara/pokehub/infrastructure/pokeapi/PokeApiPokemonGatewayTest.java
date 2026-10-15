package com.thaynara.pokehub.infrastructure.pokeapi;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.thaynara.pokehub.domain.exception.PokemonProviderUnavailableException;
import com.thaynara.pokehub.domain.model.Pokemon;
import com.thaynara.pokehub.domain.model.PokemonPage;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PokeApiPokemonGatewayTest {

    private static final String POKEMON_LIST_PATH = "/api/v2/pokemon";

    @RegisterExtension
    private static final WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Test
    void mapsListAndDetailsToAPokemonPageInListOrder() throws IOException {
        stubList(listPagePayload());
        stubBulbasaur();
        stubIvysaur();

        PokemonPage page = gateway().findPage(0, 2);

        assertThat(page.items()).containsExactly(
                new Pokemon(
                        1,
                        "bulbasaur",
                        "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png",
                        List.of("grass", "poison"),
                        69,
                        List.of("overgrow", "chlorophyll")),
                new Pokemon(
                        2,
                        "ivysaur",
                        "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/2.png",
                        List.of("grass", "poison"),
                        130,
                        List.of("overgrow", "chlorophyll")));
        assertThat(page.page()).isZero();
        assertThat(page.size()).isEqualTo(2);
        assertThat(page.totalElements()).isEqualTo(1350);
    }

    @Test
    void translatesPageAndSizeToLimitAndOffset() throws IOException {
        stubList("""
                {"count":0,"next":null,"previous":null,"results":[]}
                """);

        gateway().findPage(2, 20);

        wireMock.verify(getRequestedFor(urlPathEqualTo(POKEMON_LIST_PATH))
                .withQueryParam("limit", com.github.tomakehurst.wiremock.client.WireMock.equalTo("20"))
                .withQueryParam("offset", com.github.tomakehurst.wiremock.client.WireMock.equalTo("40")));
    }

    @Test
    void preservesListOrderWhenTheFirstDetailResponseIsSlower() throws IOException {
        stubList(listPagePayload());
        wireMock.stubFor(get(urlPathEqualTo("/api/v2/pokemon/bulbasaur"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withFixedDelay(300)
                        .withBody(fixture("pokemon-bulbasaur.json"))));
        stubIvysaur();

        PokemonPage page = gateway().findPage(0, 2);

        assertThat(page.items()).extracting(Pokemon::name).containsExactly("bulbasaur", "ivysaur");
    }

    @Test
    void returnsNoItemsButKeepsTotalElementsForAnEmptyResultsPage() {
        stubList("""
                {"count":1350,"next":null,"previous":null,"results":[]}
                """);

        PokemonPage page = gateway().findPage(0, 2);

        assertThat(page.items()).isEmpty();
        assertThat(page.totalElements()).isEqualTo(1350);
    }

    @Test
    void mapsANullFrontDefaultSpriteToANullSpriteUrl() throws IOException {
        stubList(singlePokemonListPayload());
        stubDetail("bulbasaur", fixture("pokemon-bulbasaur.json")
                .replace("\"front_default\": \"https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png\"",
                        "\"front_default\": null"));

        PokemonPage page = gateway().findPage(0, 1);

        assertThat(page.items()).singleElement()
                .extracting(Pokemon::spriteUrl)
                .isNull();
    }

    @Test
    void translatesAnHttp500FromTheListCallToProviderUnavailable() {
        wireMock.stubFor(get(urlPathEqualTo(POKEMON_LIST_PATH))
                .willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> gateway().findPage(0, 2))
                .isInstanceOf(PokemonProviderUnavailableException.class);
    }

    @Test
    void translatesAnHttp500FromADetailCallToProviderUnavailable() throws IOException {
        stubList(listPagePayload());
        stubBulbasaur();
        stubDetail("ivysaur", "", 500, 0);

        assertThatThrownBy(() -> gateway().findPage(0, 2))
                .isInstanceOf(PokemonProviderUnavailableException.class);
    }

    @Test
    void translatesAReadTimeoutToProviderUnavailable() {
        wireMock.stubFor(get(urlPathEqualTo(POKEMON_LIST_PATH))
                .willReturn(aResponse()
                        .withFixedDelay(1000)
                        .withBody("{\"count\":0,\"results\":[]}")));
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newHttpClient());
        requestFactory.setReadTimeout(Duration.ofMillis(200));
        RestClient restClient = RestClient.builder()
                .baseUrl(wireMock.baseUrl() + "/api/v2")
                .requestFactory(requestFactory)
                .build();

        assertThatThrownBy(() -> new PokeApiPokemonGateway(restClient).findPage(0, 2))
                .isInstanceOf(PokemonProviderUnavailableException.class);
    }

    private PokeApiPokemonGateway gateway() {
        return new PokeApiPokemonGateway(
                RestClient.builder().baseUrl(wireMock.baseUrl() + "/api/v2").build());
    }

    private void stubList(String body) {
        wireMock.stubFor(get(urlPathEqualTo(POKEMON_LIST_PATH)).willReturn(okJson(body)));
    }

    private void stubBulbasaur() throws IOException {
        stubDetail("bulbasaur", fixture("pokemon-bulbasaur.json"));
    }

    private void stubIvysaur() throws IOException {
        stubDetail("ivysaur", fixture("pokemon-ivysaur.json"));
    }

    private void stubDetail(String name, String body) {
        stubDetail(name, body, 200, 0);
    }

    private void stubDetail(String name, String body, int status, int delayMilliseconds) {
        wireMock.stubFor(get(urlPathEqualTo("/api/v2/pokemon/" + name))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(status)
                        .withFixedDelay(delayMilliseconds)
                        .withBody(body)));
    }

    private String listPagePayload() throws IOException {
        return fixture("pokemon-list-page0-size2.json");
    }

    private String singlePokemonListPayload() {
        return """
                {"count":1,"next":null,"previous":null,"results":[{"name":"bulbasaur","url":"https://pokeapi.co/api/v2/pokemon/1/"}]}
                """;
    }

    private String fixture(String name) throws IOException {
        try (InputStream stream = getClass().getResourceAsStream("/pokeapi/" + name)) {
            if (stream == null) {
                throw new IOException("Missing PokeAPI fixture: " + name);
            }
            return new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }
}
