package me.rudrade.todo.repository;

import static org.assertj.core.api.Assertions.*;

import me.rudrade.todo.config.SqlIntegrationTest;
import me.rudrade.todo.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

@DataJpaTest
class UserRepositoryTest extends  SqlIntegrationTest {

    @Autowired private UserRepository repository;

    @Test
    void itShouldFindByUsername() {
        User user = new User();
        user.setUsername("test-user");

        User savedUser = repository.save(user);

        Optional<User> output = repository.findByUsername("test-user");

        assertThat(output)
                .isNotEmpty()
                .isEqualTo(Optional.of(savedUser));
    }
}
