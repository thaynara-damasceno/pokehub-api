package com.thaynara.pokehub.infrastructure.security;

import com.thaynara.pokehub.application.model.AccessToken;
import com.thaynara.pokehub.application.port.out.TokenIssuer;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

public class JwtTokenIssuer implements TokenIssuer {

    private final String secret;
    private final long ttlSeconds;
    private final Clock clock;

    public JwtTokenIssuer(String secret, long ttlSeconds, Clock clock) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 bytes in UTF-8.");
        }
        if (ttlSeconds < 1) {
            throw new IllegalArgumentException("JWT TTL must be at least one second.");
        }
        this.secret = secret;
        this.ttlSeconds = ttlSeconds;
        this.clock = clock;
    }

    @Override
    public AccessToken issue(String username) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plusSeconds(ttlSeconds);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(username)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(
                new ImmutableSecret<>(secret.getBytes(StandardCharsets.UTF_8)));
        String token = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new AccessToken(token, ttlSeconds);
    }
}
