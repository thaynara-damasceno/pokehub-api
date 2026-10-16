package com.thaynara.pokehub.application.usecase;

import com.thaynara.pokehub.application.model.AccessToken;
import com.thaynara.pokehub.application.port.out.PasswordHasher;
import com.thaynara.pokehub.application.port.out.TokenIssuer;
import com.thaynara.pokehub.application.port.out.UserRepository;
import com.thaynara.pokehub.domain.exception.InvalidCredentialsException;
import com.thaynara.pokehub.domain.exception.InvalidRegistrationException;
import com.thaynara.pokehub.domain.exception.UsernameAlreadyTakenException;
import com.thaynara.pokehub.domain.model.User;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private TokenIssuer tokenIssuer;

    @InjectMocks
    private AuthService authService;

    @Test
    void registersAUserWithHashedPasswordAndNeverStoresRawPassword() {
        String rawPassword = "correct horse";
        String passwordHash = "hashed-password";
        when(userRepository.existsByUsername("trainer")).thenReturn(false);
        when(passwordHasher.hash(rawPassword)).thenReturn(passwordHash);

        authService.register("trainer", rawPassword);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().passwordHash()).isEqualTo(passwordHash);
        assertThat(userCaptor.getValue().passwordHash()).isNotEqualTo(rawPassword);
    }

    @ParameterizedTest
    @MethodSource("invalidRegistrations")
    void rejectsInvalidRegistrationDataWithoutSaving(String username, String password) {
        assertThatThrownBy(() -> authService.register(username, password))
                .isInstanceOf(InvalidRegistrationException.class)
                .hasMessageNotContaining(String.valueOf(password));

        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
    }

    static Stream<Arguments> invalidRegistrations() {
        return Stream.of(
                Arguments.of("ab", "password-123"),
                Arguments.of("u".repeat(51), "password-123"),
                Arguments.of(null, "password-123"),
                Arguments.of("trainer", "short"),
                Arguments.of("trainer", "p".repeat(73)),
                Arguments.of("trainer", null));
    }

    @Test
    void rejectsAnAlreadyTakenUsernameWithoutSaving() {
        when(userRepository.existsByUsername("trainer")).thenReturn(true);

        assertThatThrownBy(() -> authService.register("trainer", "password-123"))
                .isInstanceOf(UsernameAlreadyTakenException.class);

        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
    }

    @Test
    void returnsIssuedTokenForValidCredentials() {
        AccessToken expectedToken = new AccessToken("token-value", 900);
        when(userRepository.findByUsername("trainer"))
                .thenReturn(Optional.of(new User("trainer", "hashed-password")));
        when(passwordHasher.matches("password-123", "hashed-password")).thenReturn(true);
        when(tokenIssuer.issue("trainer")).thenReturn(expectedToken);

        AccessToken token = authService.authenticate("trainer", "password-123");

        assertThat(token).isSameAs(expectedToken);
        verify(tokenIssuer).issue("trainer");
    }

    @Test
    void returnsIdenticalInvalidCredentialsForUnknownUserAndWrongPassword() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("trainer"))
                .thenReturn(Optional.of(new User("trainer", "hashed-password")));
        when(passwordHasher.matches("wrong-password", "hashed-password")).thenReturn(false);

        Throwable unknownUserFailure = catchAuthenticationFailure("unknown", "wrong-password");
        Throwable wrongPasswordFailure = catchAuthenticationFailure("trainer", "wrong-password");

        assertThat(unknownUserFailure).isInstanceOf(InvalidCredentialsException.class);
        assertThat(wrongPasswordFailure).isInstanceOf(InvalidCredentialsException.class);
        assertThat(unknownUserFailure).hasMessage(wrongPasswordFailure.getMessage());
        verifyNoInteractions(tokenIssuer);
    }

    private Throwable catchAuthenticationFailure(String username, String password) {
        try {
            authService.authenticate(username, password);
            throw new AssertionError("Expected invalid credentials.");
        } catch (InvalidCredentialsException exception) {
            return exception;
        }
    }
}
