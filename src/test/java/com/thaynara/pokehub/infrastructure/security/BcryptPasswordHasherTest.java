package com.thaynara.pokehub.infrastructure.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BcryptPasswordHasherTest {

    private final BcryptPasswordHasher passwordHasher = new BcryptPasswordHasher();

    @Test
    void hashDoesNotEqualRawPassword() {
        String rawPassword = "correct horse battery staple";

        assertThat(passwordHasher.hash(rawPassword)).isNotEqualTo(rawPassword);
    }

    @Test
    void matchesTrueForCorrectPasswordAndFalseForWrongPassword() {
        String hash = passwordHasher.hash("correct horse battery staple");

        assertThat(passwordHasher.matches("correct horse battery staple", hash)).isTrue();
        assertThat(passwordHasher.matches("wrong password", hash)).isFalse();
    }

    @Test
    void hashesTheSamePasswordDifferentlyAndBothHashesMatch() {
        String rawPassword = "correct horse battery staple";

        String firstHash = passwordHasher.hash(rawPassword);
        String secondHash = passwordHasher.hash(rawPassword);

        assertThat(firstHash).isNotEqualTo(secondHash);
        assertThat(passwordHasher.matches(rawPassword, firstHash)).isTrue();
        assertThat(passwordHasher.matches(rawPassword, secondHash)).isTrue();
    }
}
