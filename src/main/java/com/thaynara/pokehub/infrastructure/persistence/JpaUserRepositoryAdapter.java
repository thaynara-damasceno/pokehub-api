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
        return repository.findById(username).map(this::toDomain);
    }

    @Override
    public boolean existsByUsername(String username) {
        return repository.existsById(username);
    }

    @Override
    public User save(User user) {
        return toDomain(repository.save(toEntity(user)));
    }

    private User toDomain(UserEntity entity) {
        return new User(entity.getUsername(), entity.getPasswordHash());
    }

    private UserEntity toEntity(User user) {
        UserEntity entity = new UserEntity();
        entity.setUsername(user.username());
        entity.setPasswordHash(user.passwordHash());
        return entity;
    }
}
