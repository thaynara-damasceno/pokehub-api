package com.thaynara.pokehub.infrastructure.web;

import com.thaynara.pokehub.application.model.AccessToken;
import com.thaynara.pokehub.application.port.in.AuthenticateUserUseCase;
import com.thaynara.pokehub.application.port.in.RegisterUserUseCase;
import com.thaynara.pokehub.domain.exception.InvalidCredentialsException;
import com.thaynara.pokehub.domain.exception.InvalidRegistrationException;
import com.thaynara.pokehub.domain.exception.UsernameAlreadyTakenException;
import com.thaynara.pokehub.infrastructure.security.SecurityConfig;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class AuthControllerTest {

    private static final String RAW_PASSWORD = "correct-horse-battery";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegisterUserUseCase registerUserUseCase;

    @MockitoBean
    private AuthenticateUserUseCase authenticateUserUseCase;

    @Test
    void registersUserAndReturnsCreatedWithNoBody() throws Exception {
        var result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"trainer","password":"%s"}
                                """.formatted(RAW_PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(content().string(""));

        verify(registerUserUseCase).register("trainer", RAW_PASSWORD);
        assertResponseDoesNotExposeCredentials(result.andReturn().getResponse().getContentAsString());
    }

    @Test
    void returnsConflictProblemDetailsWhenUsernameIsTaken() throws Exception {
        doThrow(new UsernameAlreadyTakenException("Username is already taken."))
                .when(registerUserUseCase).register("trainer", RAW_PASSWORD);
        var result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"trainer","password":"%s"}
                                """.formatted(RAW_PASSWORD)))
                .andExpect(status().isConflict())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.status").value(409));

        assertResponseDoesNotExposeCredentials(result.andReturn().getResponse().getContentAsString());
    }

    @Test
    void returnsBadRequestProblemDetailsWhenRegistrationIsInvalid() throws Exception {
        doThrow(new InvalidRegistrationException(
                "Username must be between 3 and 50 characters."))
                .when(registerUserUseCase).register("trainer", RAW_PASSWORD);
        var result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"trainer","password":"%s"}
                                """.formatted(RAW_PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.status").value(400));

        assertResponseDoesNotExposeCredentials(result.andReturn().getResponse().getContentAsString());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("registrationValidationCases")
    void rejectsInvalidRegistrationRequest(String displayName, String requestBody, String field)
            throws Exception {
        assertRegistrationValidationError(requestBody, field);
        verifyNoInteractions(registerUserUseCase);
    }

    private static Stream<Arguments> registrationValidationCases() {
        return Stream.of(
                Arguments.of("registration: missing username", """
                        {"password":"%s"}
                        """.formatted(RAW_PASSWORD), "username"),
                Arguments.of("registration: blank username", """
                        {"username":" ","password":"%s"}
                        """.formatted(RAW_PASSWORD), "username"),
                Arguments.of("registration: missing password", """
                        {"username":"trainer"}
                        """, "password"),
                Arguments.of("registration: blank password", """
                        {"username":"trainer","password":" "}
                        """, "password"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("frameworkErrorCases")
    void returnsProblemDetailsForRegistrationFrameworkErrors(
            String displayName, String requestBody, String contentType, int expectedStatus)
            throws Exception {
        MockHttpServletRequestBuilder request = post("/api/auth/register")
                .contentType(MediaType.valueOf(contentType))
                .content(requestBody);

        assertProblemDetails(request, expectedStatus);
    }

    private static Stream<Arguments> frameworkErrorCases() {
        return Stream.of(
                Arguments.of("registration: malformed JSON", "{\"username\":",
                        "application/json", 400),
                Arguments.of("registration: empty body", "",
                        "application/json", 400),
                Arguments.of("registration: unsupported content type", """
                        {"username":"trainer","password":"correct-horse-battery"}
                        """, "text/plain", 415));
    }

    @Test
    void logsInAndReturnsBearerTokenAndExpiration() throws Exception {
        when(authenticateUserUseCase.authenticate("trainer", RAW_PASSWORD))
                .thenReturn(new AccessToken("issued-token", 3600));
        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"trainer","password":"%s"}
                                """.formatted(RAW_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken").value("issued-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600));

        verify(authenticateUserUseCase).authenticate("trainer", RAW_PASSWORD);
        assertResponseDoesNotExposeCredentials(result.andReturn().getResponse().getContentAsString());
    }

    @Test
    void returnsUnauthorizedProblemDetailsWhenCredentialsAreInvalid() throws Exception {
        when(authenticateUserUseCase.authenticate("trainer", RAW_PASSWORD))
                .thenThrow(new InvalidCredentialsException());
        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"trainer","password":"%s"}
                                """.formatted(RAW_PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Invalid username or password."));

        assertResponseDoesNotExposeCredentials(result.andReturn().getResponse().getContentAsString());
    }

    private void assertResponseDoesNotExposeCredentials(String responseBody) {
        assertThat(responseBody)
                .doesNotContain(RAW_PASSWORD)
                .doesNotContain("passwordHash");
    }

    private void assertRegistrationValidationError(String requestBody, String field)
            throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0].field").value(field))
                .andExpect(jsonPath("$.errors[0].message").isNotEmpty());
    }

    private void assertProblemDetails(MockHttpServletRequestBuilder request, int expectedStatus)
            throws Exception {
        mockMvc.perform(request)
                .andExpect(status().is(expectedStatus))
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.status").value(expectedStatus))
                .andExpect(jsonPath("$.title").isNotEmpty());
    }
}
