package com.thaynara.pokehub.infrastructure.web;

import com.jayway.jsonpath.JsonPath;
import com.thaynara.pokehub.application.port.in.GetPokemonDetailUseCase;
import com.thaynara.pokehub.application.port.out.TokenIssuer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class InternalServerErrorResponseTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TokenIssuer tokenIssuer;

    @MockitoBean
    private GetPokemonDetailUseCase getPokemonDetailUseCase;

    @ParameterizedTest(name = "{0}")
    @MethodSource("internalErrorCases")
    void returnsSanitizedProblemDetailsWhenUseCaseFails(String displayName) throws Exception {
        when(getPokemonDetailUseCase.get(25))
                .thenThrow(new RuntimeException("secret internal detail"));

        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/pokemon/25"))
                        .header("Authorization", "Bearer " + tokenIssuer.issue("test").value())
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(response.headers().firstValue("Content-Type").orElse(null))
                .isEqualTo(MediaType.valueOf("application/problem+json").toString());
        assertThat(JsonPath.parse(response.body()).read("$.status", Integer.class)).isEqualTo(500);
        assertThat(JsonPath.parse(response.body()).read("$.title", String.class)).isNotEmpty();
        assertThat(response.body()).doesNotContain("secret internal detail");
    }

    private static Stream<Arguments> internalErrorCases() {
        return Stream.of(Arguments.of("public listing: internal use-case failure"));
    }
}
