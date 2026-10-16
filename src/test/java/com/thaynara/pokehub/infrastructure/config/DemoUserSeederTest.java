package com.thaynara.pokehub.infrastructure.config;

import com.thaynara.pokehub.application.port.in.RegisterUserUseCase;
import com.thaynara.pokehub.domain.exception.InvalidRegistrationException;
import com.thaynara.pokehub.domain.exception.UsernameAlreadyTakenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DemoUserSeederTest {

    private static final String USERNAME = "demo-trainer";
    private static final String PASSWORD = "demo-password-123";

    @Mock
    private RegisterUserUseCase registerUserUseCase;

    private DemoUserSeeder demoUserSeeder;

    @BeforeEach
    void createSeeder() {
        demoUserSeeder = new DemoUserSeeder(registerUserUseCase, USERNAME, PASSWORD);
    }

    @Test
    void registersTheConfiguredUsernameAndPassword() {
        demoUserSeeder.run();

        verify(registerUserUseCase).register(USERNAME, PASSWORD);
    }

    @Test
    void ignoresAnAlreadyTakenUsername() {
        doThrow(new UsernameAlreadyTakenException("Username is already taken."))
                .when(registerUserUseCase).register(USERNAME, PASSWORD);

        assertThatCode(() -> demoUserSeeder.run()).doesNotThrowAnyException();
    }

    @Test
    void propagatesInvalidRegistration() {
        InvalidRegistrationException failure =
                new InvalidRegistrationException("Invalid registration.");
        doThrow(failure).when(registerUserUseCase).register(USERNAME, PASSWORD);

        assertThatThrownBy(() -> demoUserSeeder.run())
                .isSameAs(failure);
    }
}
