package com.thaynara.pokehub.application.port.in;

import com.thaynara.pokehub.application.model.AccessToken;

public interface AuthenticateUserUseCase {

    AccessToken authenticate(String username, String rawPassword);
}
