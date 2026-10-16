package com.thaynara.pokehub.application.port.out;

import com.thaynara.pokehub.application.model.AccessToken;

public interface TokenIssuer {

    AccessToken issue(String username);
}
