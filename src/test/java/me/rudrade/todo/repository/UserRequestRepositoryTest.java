package me.rudrade.todo.repository;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;

import me.rudrade.todo.config.ConfigurationUtil;
import me.rudrade.todo.config.SqlIntegrationTest;
import me.rudrade.todo.model.UserRequest;
import me.rudrade.todo.model.types.Role;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;

@DataJpaTest(includeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = Service.class))
@Import({ConfigurationUtil.PasswordEncoder.class, ConfigurationUtil.MailSender.class})
class UserRequestRepositoryTest extends SqlIntegrationTest {

    @Autowired private UserRequestRepository repository;

    private UserRequest saveUserRequest(String username, String email) {
        UserRequest request = new UserRequest();
        request.setUsername(username);
        request.setPassword("password");
        request.setEmail(email);
        request.setRole(Role.ROLE_USER);
        request.setDtCreated(LocalDateTime.now());
        return repository.save(request);
    }

    @Test
    void itShouldReturnTrueWhenUsernameExists() {
        saveUserRequest("new-user", "new-user@mail.com");

        boolean exists = repository.existsByUsernameOrEmail("new-user", "other@mail.com");

        assertThat(exists).isTrue();
    }

    @Test
    void itShouldReturnTrueWhenEmailExists() {
        saveUserRequest("another-user", "unique@mail.com");

        boolean exists = repository.existsByUsernameOrEmail("missing-user", "unique@mail.com");

        assertThat(exists).isTrue();
    }

    @Test
    void itShouldReturnFalseWhenUsernameAndEmailDoNotExist() {
        saveUserRequest("present-user", "present@mail.com");

        boolean exists = repository.existsByUsernameOrEmail("absent-user", "absent@mail.com");

        assertThat(exists).isFalse();
    }

    @Test
    void itShouldDeleteIfExpired() {
        var userRequest = new UserRequest();
        userRequest.setDtCreated(LocalDateTime.now().minusMinutes(61));
        userRequest.setRole(Role.ROLE_USER);
        userRequest.setPassword("test");
        userRequest.setUsername("user-expired");
        userRequest.setEmail("email-expired@test.com");
        repository.save(userRequest);

        repository.deleteIfExpired(60);

        var dbRequest = repository.findById(userRequest.getId());
        assertThat(dbRequest).isNotPresent();
    }

    @Test
    void itShouldFindAllByMailSentIsFalse() {
        var userRequest1 = new UserRequest();
        userRequest1.setDtCreated(LocalDateTime.now());
        userRequest1.setRole(Role.ROLE_USER);
        userRequest1.setPassword("test");
        userRequest1.setUsername("user-mail-1");
        userRequest1.setEmail("user-mail-1@test.com");
        repository.save(userRequest1);

        var userRequest2 = new UserRequest();
        userRequest2.setDtCreated(LocalDateTime.now());
        userRequest2.setRole(Role.ROLE_USER);
        userRequest2.setPassword("test");
        userRequest2.setUsername("user-mail-2");
        userRequest2.setEmail("user-mail-2@test.com");
        repository.save(userRequest2);

        var result = repository.findAllByMailSentIsFalse();
        assertThat(result)
            .hasSize(2)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(userRequest1, userRequest2);
    }

    @Test
    void itShouldFindRequestByUsername() {
        var userRequest1 = new UserRequest();
        userRequest1.setDtCreated(LocalDateTime.now());
        userRequest1.setRole(Role.ROLE_USER);
        userRequest1.setPassword("test");
        userRequest1.setUsername("user-abc-3");
        userRequest1.setEmail("user-mail-3@test.com");
        repository.save(userRequest1);

        var userRequest2 = new UserRequest();
        userRequest2.setDtCreated(LocalDateTime.now());
        userRequest2.setRole(Role.ROLE_USER);
        userRequest2.setPassword("test");
        userRequest2.setUsername("User-ABC-4");
        userRequest2.setEmail("user-mail-4@test.com");
        repository.save(userRequest2);

        var userRequest3 = new UserRequest();
        userRequest3.setDtCreated(LocalDateTime.now());
        userRequest3.setRole(Role.ROLE_USER);
        userRequest3.setPassword("test");
        userRequest3.setUsername("user-other-5");
        userRequest3.setEmail("user-mail-5@test.com");
        repository.save(userRequest3);

        var result = repository.findAllByUsernameContainingIgnoringCase("abc");
        assertThat(result)
            .hasSize(2)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(userRequest1, userRequest2);
    }

    @Test
    void itShouldFindRequestByEmail() {
        var userRequest1 = new UserRequest();
        userRequest1.setDtCreated(LocalDateTime.now());
        userRequest1.setRole(Role.ROLE_USER);
        userRequest1.setPassword("test");
        userRequest1.setUsername("user-email-1");
        userRequest1.setEmail("person@example.com");
        repository.save(userRequest1);

        var userRequest2 = new UserRequest();
        userRequest2.setDtCreated(LocalDateTime.now());
        userRequest2.setRole(Role.ROLE_USER);
        userRequest2.setPassword("test");
        userRequest2.setUsername("user-email-2");
        userRequest2.setEmail("another@Example.com");
        repository.save(userRequest2);

        var userRequest3 = new UserRequest();
        userRequest3.setDtCreated(LocalDateTime.now());
        userRequest3.setRole(Role.ROLE_USER);
        userRequest3.setPassword("test");
        userRequest3.setUsername("user-email-3");
        userRequest3.setEmail("nomatch@test.org");
        repository.save(userRequest3);

        var result = repository.findAllByEmailContainingIgnoringCase("example.com");
        assertThat(result)
            .hasSize(2)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(userRequest1, userRequest2);
    }
}

