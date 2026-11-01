package com.thaynara.pokehub.infrastructure.web;

import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank String username,
        @NotBlank String password) {
}
