package com.thaynara.pokehub.application.usecase;

import com.thaynara.pokehub.application.model.AccessToken;
import com.thaynara.pokehub.application.port.in.AuthenticateUserUseCase;
import com.thaynara.pokehub.application.port.in.RegisterUserUseCase;
import com.thaynara.pokehub.application.port.out.PasswordHasher;
import com.thaynara.pokehub.application.port.out.TokenIssuer;
import com.thaynara.pokehub.application.port.out.UserRepository;

public class AuthService implements RegisterUserUseCase, AuthenticateUserUseCase {

    public AuthService(UserRepository userRepository, PasswordHasher passwordHasher, TokenIssuer tokenIssuer) {
    }

    @Override
    public void register(String username, String rawPassword) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AccessToken authenticate(String username, String rawPassword) {
        throw new UnsupportedOperationException();
    }
}
