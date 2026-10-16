package com.thaynara.pokehub.infrastructure.security;

import com.thaynara.pokehub.application.model.AccessToken;
import com.thaynara.pokehub.application.port.out.TokenIssuer;
import java.time.Clock;

public class JwtTokenIssuer implements TokenIssuer {

    private final String secret;
    private final long ttlSeconds;
    private final Clock clock;

    public JwtTokenIssuer(String secret, long ttlSeconds, Clock clock) {
        this.secret = secret;
        this.ttlSeconds = ttlSeconds;
        this.clock = clock;
    }

    @Override
    public AccessToken issue(String username) {
        throw new UnsupportedOperationException();
    }
}
