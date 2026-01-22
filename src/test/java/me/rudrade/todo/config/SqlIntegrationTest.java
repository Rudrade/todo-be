package me.rudrade.todo.config;

import me.rudrade.todo.model.User;
import me.rudrade.todo.model.types.Language;
import me.rudrade.todo.model.types.Role;
import me.rudrade.todo.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.lettuce.core.RedisClient;

import java.util.Optional;
import java.util.UUID;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
public abstract class SqlIntegrationTest {

    public static final String TEST_USERNAME = "valid-user";
    public static final String TEST_PASSWORD = "test123";
    public static final String TEST_EMAIL = "test@mail.com";

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

    @Autowired private UserRepository userRepository;
    @Autowired public PasswordEncoder encoder;

    public User getTestUser() {
        Optional<User> user = userRepository.findByUsername(TEST_USERNAME);
        if (user.isEmpty()) {
            var adminTmp = new User();
            adminTmp.setRole(Role.ROLE_ADMIN);

            var userTmp = new User();
            userTmp.setActive(true);
            userTmp.setEmail(TEST_EMAIL);
            userTmp.setUsername(TEST_USERNAME);
            userTmp.setPassword(encoder.encode(TEST_PASSWORD));
            userTmp.setRole(Role.ROLE_USER);
            userTmp.setLanguage(Language.EN);

            userRepository.save(userTmp);
            user = userRepository.findByUsername(TEST_USERNAME);
        } else if (user.get().getPassword().isBlank() || user.get().getPassword().equals("change")) {
            user.get().setPassword(encoder.encode(TEST_PASSWORD));
            userRepository.save(user.get());
        }

        return user.orElseThrow();
    }

    public boolean sameUser(User a, User b) {
        return a != null && b != null && a.getId() != null && a.getId().equals(b.getId());
    }

    public User createUser() {
        var user = new User();
        user.setUsername(UUID.randomUUID().toString());
        user.setPassword(encoder.encode("test"));
        user.setEmail(user.getUsername()+"@t");
        user.setRole(Role.ROLE_USER);
        user.setLanguage(Language.EN);
        return user;
    }

    @MockitoBean private RedisClient client;
    @MockitoBean private ProxyManager<String> proxyManager;

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
