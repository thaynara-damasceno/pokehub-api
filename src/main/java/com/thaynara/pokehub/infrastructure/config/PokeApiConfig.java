package com.thaynara.pokehub.infrastructure.config;

import com.thaynara.pokehub.application.port.out.PokemonGateway;
import com.thaynara.pokehub.infrastructure.pokeapi.CachingPokemonGateway;
import com.thaynara.pokehub.infrastructure.pokeapi.PokeApiPokemonGateway;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class PokeApiConfig {

    @Bean
    RestClient pokeApiRestClient(
            @Value("${pokeapi.base-url}") String baseUrl,
            @Value("${pokeapi.connect-timeout-ms}") int connectTimeoutMs,
            @Value("${pokeapi.read-timeout-ms}") int readTimeoutMs) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Bean
    PokemonGateway pokemonGateway(
            RestClient pokeApiRestClient,
            @Value("${pokeapi.cache.ttl-seconds:300}") long ttlSeconds,
            @Value("${pokeapi.cache.max-size:500}") long maxSize) {
        return new CachingPokemonGateway(
                new PokeApiPokemonGateway(pokeApiRestClient),
                Duration.ofSeconds(ttlSeconds),
                maxSize,
                System::nanoTime);
    }
}
