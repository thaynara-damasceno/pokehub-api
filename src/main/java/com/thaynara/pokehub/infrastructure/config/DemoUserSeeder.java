package com.thaynara.pokehub.infrastructure.config;

import com.thaynara.pokehub.application.port.in.RegisterUserUseCase;
import org.springframework.boot.CommandLineRunner;

public class DemoUserSeeder implements CommandLineRunner {

    private final RegisterUserUseCase registerUserUseCase;
    private final String username;
    private final String password;

    public DemoUserSeeder(
            RegisterUserUseCase registerUserUseCase,
            String username,
            String password) {
        this.registerUserUseCase = registerUserUseCase;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(String... args) {
    }
}
