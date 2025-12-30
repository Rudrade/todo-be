package me.rudrade.todo.repository;

import static org.assertj.core.api.Assertions.*;

import me.rudrade.todo.config.SqlIntegrationTest;
import me.rudrade.todo.model.User;
import me.rudrade.todo.model.types.Role;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.stereotype.Service;

import java.util.Optional;

@DataJpaTest(includeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = Service.class))
class UserRepositoryTest extends  SqlIntegrationTest {

    @Autowired private UserRepository repository;

    @Test
    void itShouldFindByUsername() {
        User user = new User();
        user.setUsername("test-user");
        user.setPassword("test");
        user.setRole(Role.ROLE_USER);
        user.setEmail("test-user@test.com");

        User savedUser = repository.save(user);

        Optional<User> output = repository.findByUsername("test-user");

        assertThat(output)
                .isNotEmpty()
                .isEqualTo(Optional.of(savedUser));
    }
}
