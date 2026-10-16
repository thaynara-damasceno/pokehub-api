package com.thaynara.pokehub.application.port.in;

public interface RegisterUserUseCase {

    void register(String username, String rawPassword);
}
