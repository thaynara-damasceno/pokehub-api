package com.thaynara.pokehub.application.port.out;

import com.thaynara.pokehub.domain.model.User;
import java.util.Optional;

public interface UserRepository {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    User save(User user);
}
