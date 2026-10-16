package com.thaynara.pokehub.infrastructure.config;

import com.thaynara.pokehub.application.port.in.RegisterUserUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class DemoUserSeedConfig {

    @Bean
    @Profile("!test")
    @ConditionalOnProperty(name = "pokehub.seed.enabled", havingValue = "true")
    DemoUserSeeder demoUserSeeder(
            RegisterUserUseCase registerUserUseCase,
            @Value("${pokehub.seed.username}") String username,
            @Value("${pokehub.seed.password}") String password) {
        return new DemoUserSeeder(registerUserUseCase, username, password);
    }
}
