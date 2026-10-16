package com.thaynara.pokehub.infrastructure.config;

import com.thaynara.pokehub.application.port.in.RegisterUserUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DemoUserSeedConfig {

    @Bean
    DemoUserSeeder demoUserSeeder(
            RegisterUserUseCase registerUserUseCase,
            @Value("${pokehub.seed.username:}") String username,
            @Value("${pokehub.seed.password:}") String password) {
        return new DemoUserSeeder(registerUserUseCase, username, password);
    }
}
