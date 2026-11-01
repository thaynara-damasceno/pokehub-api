package com.thaynara.pokehub.infrastructure.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;

public class ProblemDetailAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final byte[] PROBLEM_DETAIL = """
            {"status":401,"title":"Unauthorized","detail":"Authentication is required or the bearer token is invalid."}
            """.strip().getBytes(StandardCharsets.UTF_8);

    private final BearerTokenAuthenticationEntryPoint bearerTokenAuthenticationEntryPoint =
            new BearerTokenAuthenticationEntryPoint();

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {
        bearerTokenAuthenticationEntryPoint.commence(request, response, authException);

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getOutputStream().write(PROBLEM_DETAIL);
    }
}
