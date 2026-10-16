package com.thaynara.pokehub.application.usecase;

import com.thaynara.pokehub.application.model.AccessToken;
import com.thaynara.pokehub.application.port.in.AuthenticateUserUseCase;
import com.thaynara.pokehub.application.port.in.RegisterUserUseCase;
import com.thaynara.pokehub.application.port.out.PasswordHasher;
import com.thaynara.pokehub.application.port.out.TokenIssuer;
import com.thaynara.pokehub.application.port.out.UserRepository;
import com.thaynara.pokehub.domain.exception.InvalidCredentialsException;
import com.thaynara.pokehub.domain.exception.InvalidRegistrationException;
import com.thaynara.pokehub.domain.exception.UsernameAlreadyTakenException;
import com.thaynara.pokehub.domain.model.User;
import java.util.Optional;

public class AuthService implements RegisterUserUseCase, AuthenticateUserUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final TokenIssuer tokenIssuer;

    public AuthService(
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            TokenIssuer tokenIssuer) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer = tokenIssuer;
    }

    @Override
    public void register(String username, String rawPassword) {
        String normalizedUsername = username == null ? null : username.trim();
        if (normalizedUsername == null
                || normalizedUsername.length() < 3
                || normalizedUsername.length() > 50) {
            throw new InvalidRegistrationException("Username must be between 3 and 50 characters.");
        }
        if (rawPassword == null || rawPassword.length() < 8 || rawPassword.length() > 72) {
            throw new InvalidRegistrationException("Password must be between 8 and 72 characters.");
        }
        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new UsernameAlreadyTakenException("Username is already taken.");
        }

        userRepository.save(new User(normalizedUsername, passwordHasher.hash(rawPassword)));
    }

    @Override
    public AccessToken authenticate(String username, String rawPassword) {
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isEmpty() || !passwordHasher.matches(rawPassword, user.get().passwordHash())) {
            throw new InvalidCredentialsException();
        }
        return tokenIssuer.issue(user.get().username());
    }
}
