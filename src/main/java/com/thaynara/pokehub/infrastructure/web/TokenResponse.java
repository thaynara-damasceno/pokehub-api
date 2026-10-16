package com.thaynara.pokehub.infrastructure.web;

import com.thaynara.pokehub.application.model.AccessToken;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {

    public static TokenResponse from(AccessToken accessToken) {
        return new TokenResponse(accessToken.value(), "Bearer", accessToken.expiresInSeconds());
    }
}
