package com.thaynara.pokehub.infrastructure.security;

import com.thaynara.pokehub.application.port.out.PasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BcryptPasswordHasher implements PasswordHasher {

    @Override
    public String hash(String rawPassword) {
        return new BCryptPasswordEncoder().encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        return new BCryptPasswordEncoder().matches(rawPassword, passwordHash);
    }
}
