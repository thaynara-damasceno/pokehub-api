package com.thaynara.pokehub.infrastructure.persistence;

import com.thaynara.pokehub.application.port.out.UserRepository;
import com.thaynara.pokehub.domain.model.User;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaUserRepositoryAdapter implements UserRepository {

    private final SpringDataUserRepository repository;

    public JpaUserRepositoryAdapter(SpringDataUserRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean existsByUsername(String username) {
        throw new UnsupportedOperationException();
    }

    @Override
    public User save(User user) {
        throw new UnsupportedOperationException();
    }
}
