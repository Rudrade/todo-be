package me.rudrade.todo.repository;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import me.rudrade.todo.config.ConfigurationUtil;
import me.rudrade.todo.config.SqlIntegrationTest;
import me.rudrade.todo.model.User;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;

@Import({ConfigurationUtil.PasswordEncoder.class, ConfigurationUtil.MailSender.class})
@DataJpaTest(includeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = Service.class))
@Sql(scripts = "/sql-scripts/INIT_USERS.sql", executionPhase = ExecutionPhase.BEFORE_TEST_CLASS)
class UserRepositoryTest extends  SqlIntegrationTest {

    @Autowired private UserRepository repository;

    @Test
    void itShouldFindByUsername() {
        var output = repository.findByUsername("test-user");

        assertThat(output)
                .isPresent()
                .satisfies(user -> {
                    assertThat(user.get().getUsername()).isEqualTo("test-user");
                });
    }

    @ParameterizedTest
    @CsvSource({
        "test-user,null",
        "null,test-mail@test",
        "test-user,test-mail-2@test"
    })
    void itShouldFindActiveByUsernameOrEmail(String username, String email) {
        var result = repository.findActiveByUsernameOrEmail(username, email);
        assertThat(result).isNotEmpty();

        List<User> expected = new ArrayList<>();
        repository.findAll().forEach(user -> {
            if (username != null && user.getUsername().equals(username)) {
                expected.add(user);
            } else if (email != null && user.getEmail().equals(email)) {
                expected.add(user);
            }
        });

        assertThat(result).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    void itShouldFindByActive() {
        var result = repository.findByActive(true);
        assertThat(result).isNotEmpty();

        List<User> expected = new ArrayList<>();
        repository.findAll().forEach(user -> {
            if (user.isActive()) {
                expected.add(user);
            }
        });

        assertThat(result).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    void itShouldFindByUsernameContainingIgnoreCase() {
        var result = repository.findByUsernameContainingIgnoreCase("TEST-uSeR");
        assertThat(result).isNotEmpty();

        List<User> expected = new ArrayList<>();
        repository.findAll().forEach(user -> {
            if (user.getUsername().toLowerCase().contains("test-user")) {
                expected.add(user);
            }
        });

        assertThat(result).containsExactlyInAnyOrderElementsOf(expected);

    }

    @Test
    void itShouldFindByEmailContainingIgnoreCase() {
        var result =  repository.findByEmailContainingIgnoreCase("test-MAil");
        assertThat(result).isNotEmpty();

        List<User> expected = new ArrayList<>();
        repository.findAll().forEach(user -> {
            if (user.getEmail().toLowerCase().contains("test-mail")) {
                expected.add(user);
            }
        });
        
        assertThat(result).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    void itShouldFindByActiveAndUsernameContainingIgnoreCase() {
        var result = repository.findByActiveAndUsernameContainingIgnoreCase(true, "test-USER");
        assertThat(result).isNotEmpty();

        List<User> expected = new ArrayList<>();
        repository.findAll().forEach(user -> {
            if (user.isActive() && user.getUsername().toLowerCase().contains("test-user")) {
                expected.add(user);
            }
        });

        assertThat(result).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    void itShouldFindByActiveAndEmailContainingIgnoreCase() {
        var result = repository.findByActiveAndEmailContainingIgnoreCase(true, "TEST-maiL");
        assertThat(result).isNotEmpty();

        List<User> expected = new ArrayList<>();
        repository.findAll().forEach(user -> {
            if (user.isActive() && user.getEmail().toLowerCase().contains("test-mail")) {
                expected.add(user);
            }
        });

        assertThat(result).containsExactlyInAnyOrderElementsOf(expected);
    }

}
