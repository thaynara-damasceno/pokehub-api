package com.thaynara.pokehub.infrastructure.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.SignedJWT;
import com.thaynara.pokehub.application.model.AccessToken;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenIssuerTest {

    private static final String SECRET = "a-test-secret-key-with-at-least-32-bytes";
    private static final byte[] SECRET_BYTES = SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    private static final Instant ISSUED_AT = Instant.parse("2026-01-15T10:30:00Z");
    private static final long TTL_SECONDS = 900;
    private static final Clock FIXED_CLOCK = Clock.fixed(ISSUED_AT, ZoneOffset.UTC);

    @Test
    void issuesTokenWithSubjectIssueTimeExpirationAndTtl() {
        AccessToken accessToken = new JwtTokenIssuer(SECRET, TTL_SECONDS, FIXED_CLOCK)
                .issue("trainer");
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(
                        new SecretKeySpec(SECRET_BYTES, "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        Jwt jwt = decoder.decode(accessToken.value());

        assertThat(jwt.getSubject()).isEqualTo("trainer");
        assertThat(jwt.getIssuedAt()).isEqualTo(ISSUED_AT);
        assertThat(jwt.getExpiresAt()).isEqualTo(ISSUED_AT.plusSeconds(TTL_SECONDS));
        assertThat(accessToken.expiresInSeconds()).isEqualTo(TTL_SECONDS);
    }

    @Test
    void rejectsTokenWhenDecodedWithDifferentSecret() {
        AccessToken accessToken = new JwtTokenIssuer(SECRET, TTL_SECONDS, FIXED_CLOCK)
                .issue("trainer");
        byte[] differentSecret = "a-different-secret-key-of-32-bytes".getBytes(
                java.nio.charset.StandardCharsets.UTF_8);
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(
                        new SecretKeySpec(differentSecret, "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        assertThatThrownBy(() -> decoder.decode(accessToken.value()))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsSecretShorterThanThirtyTwoBytes() {
        assertThatThrownBy(() -> new JwtTokenIssuer("short-secret", TTL_SECONDS, FIXED_CLOCK))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullSecret() {
        assertThatThrownBy(() -> new JwtTokenIssuer(null, TTL_SECONDS, FIXED_CLOCK))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsTtlLessThanOneSecond() {
        assertThatThrownBy(() -> new JwtTokenIssuer(SECRET, 0, FIXED_CLOCK))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
