package com.thaynara.pokehub.infrastructure.persistence;

import com.thaynara.pokehub.domain.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaUserRepositoryAdapter.class)
class JpaUserRepositoryAdapterTest {

    @Autowired
    private JpaUserRepositoryAdapter adapter;

    @Test
    void savesAndFindsUsernameAndPasswordHash() {
        User user = new User("trainer", "hashed-password");

        adapter.save(user);

        assertThat(adapter.findByUsername("trainer")).contains(user);
    }

    @Test
    void returnsEmptyWhenUsernameIsUnknown() {
        assertThat(adapter.findByUsername("unknown")).isEmpty();
    }

    @Test
    void reportsWhetherUsernameExists() {
        adapter.save(new User("trainer", "hashed-password"));

        assertThat(adapter.existsByUsername("trainer")).isTrue();
        assertThat(adapter.existsByUsername("unknown")).isFalse();
    }

    @Test
    void savesTwoDifferentUsers() {
        User first = new User("trainer-one", "hash-one");
        User second = new User("trainer-two", "hash-two");
        adapter.save(first);
        adapter.save(second);

        assertThat(adapter.findByUsername("trainer-one")).contains(first);
        assertThat(adapter.findByUsername("trainer-two")).contains(second);
    }
}
