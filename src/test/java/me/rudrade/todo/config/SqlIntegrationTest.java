package me.rudrade.todo.config;

import me.rudrade.todo.dto.UserDto;
import me.rudrade.todo.model.User;
import me.rudrade.todo.repository.UserRepository;
import me.rudrade.todo.service.AuthenticationService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
public abstract class SqlIntegrationTest {

    public static final String TEST_USERNAME = "valid-user";
    public static final String TEST_PASSWORD = "test123";

    @Container
    static SqlContainer sqlContainer = new SqlContainer();

    static {
        sqlContainer.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", sqlContainer::getJdbcUrl);
        registry.add("spring.datasource.username", sqlContainer::getUsername);
        registry.add("spring.datasource.password", sqlContainer::getPassword);

        registry.add("db_url", sqlContainer::getJdbcUrl);
        registry.add("db_username", sqlContainer::getUsername);
        registry.add("db_password", sqlContainer::getPassword);
    }

    @Autowired private AuthenticationService authenticationService;
    @Autowired private UserRepository userRepository;

    public User getTestUser() {
        Optional<User> user = userRepository.findByUsername(TEST_USERNAME);
        if (user.isEmpty()) {
            authenticationService.createUser(new UserDto(TEST_USERNAME, TEST_PASSWORD));
            user = userRepository.findByUsername(TEST_USERNAME);
        } else if (user.get().getPassword().isBlank()) {
            user.get().setPassword(PasswordEncoderFactories.createDelegatingPasswordEncoder().encode(TEST_PASSWORD));
            userRepository.save(user.get());
        }

        return user.orElseThrow();
    }

    public boolean sameUser(User a, User b) {
        return a != null && b != null && a.getId() != null && a.getId().equals(b.getId());
    }

}

@SuppressWarnings("rawtypes")
class SqlContainer extends MySQLContainer {

    SqlContainer() {
        super("mysql:8.0");
    }

    @Override
    public void stop() {
        // Do nonthing, JVM closes
    }
}
