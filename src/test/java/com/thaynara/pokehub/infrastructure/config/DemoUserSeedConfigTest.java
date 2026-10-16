package com.thaynara.pokehub.infrastructure.config;

import com.thaynara.pokehub.application.port.in.RegisterUserUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DemoUserSeedConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(DemoUserSeedConfig.class)
            .withBean(RegisterUserUseCase.class, () -> mock(RegisterUserUseCase.class))
            .withPropertyValues(
                    "pokehub.seed.username=demo-trainer",
                    "pokehub.seed.password=demo-password-123");

    @Test
    void createsDemoUserSeederWhenSeedIsEnabled() {
        contextRunner
                .withPropertyValues("pokehub.seed.enabled=true")
                .run(context -> assertThat(context).hasSingleBean(DemoUserSeeder.class));
    }

    @Test
    void doesNotCreateDemoUserSeederWhenSeedIsDisabled() {
        contextRunner
                .withPropertyValues("pokehub.seed.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(DemoUserSeeder.class));
    }
}
