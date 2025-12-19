package me.rudrade.todo.config;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
public abstract class SqlIntegrationTest {

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

}

class SqlContainer extends MySQLContainer {

    SqlContainer() {
        super("mysql:8.0");
    }

    @Override
    public void stop() {
        // Do nonthing, JVM closes
    }
}
