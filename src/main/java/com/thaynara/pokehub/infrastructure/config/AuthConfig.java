package com.thaynara.pokehub.infrastructure.config;

import com.thaynara.pokehub.application.port.in.AuthenticateUserUseCase;
import com.thaynara.pokehub.application.port.in.RegisterUserUseCase;
import com.thaynara.pokehub.application.port.out.PasswordHasher;
import com.thaynara.pokehub.application.port.out.TokenIssuer;
import com.thaynara.pokehub.application.port.out.UserRepository;
import com.thaynara.pokehub.application.usecase.AuthService;
import com.thaynara.pokehub.infrastructure.security.BcryptPasswordHasher;
import com.thaynara.pokehub.infrastructure.security.JwtTokenIssuer;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    PasswordHasher passwordHasher() {
        return new BcryptPasswordHasher();
    }

    @Bean
    TokenIssuer tokenIssuer(
            @Value("${pokehub.jwt.secret}") String secret,
            @Value("${pokehub.jwt.ttl-seconds}") long ttlSeconds,
            Clock clock) {
        return new JwtTokenIssuer(secret, ttlSeconds, clock);
    }

    @Bean
    AuthService authService(
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            TokenIssuer tokenIssuer) {
        return new AuthService(userRepository, passwordHasher, tokenIssuer);
    }
}
