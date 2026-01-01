package me.rudrade.todo.repository;

import me.rudrade.todo.config.ConfigurationUtil;
import me.rudrade.todo.config.SqlIntegrationTest;
import me.rudrade.todo.model.UserList;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.springframework.test.context.jdbc.Sql;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest(includeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = Service.class))
@Sql("/sql-scripts/INIT_USER_LISTS.sql")
@Import(ConfigurationUtil.class)
class UserListRepositoryTest extends SqlIntegrationTest {

    @Autowired private UserListRepository repository;

    private List<UserList> getLists() {
        List<UserList> lists = new ArrayList<>();
        repository.findAll().forEach(lists::add);
        assertThat(lists).as("No user lists present in DB.").isNotEmpty();
        return lists;
    }

    @Test
    void itShouldFindByNameAndUserId() {
        var user = getTestUser();
        var expected = getLists().stream()
            .filter(ul -> sameUser(ul.getUser(), user))
            .findFirst()
            .orElseThrow();

        var result = repository.findByNameAndUserId(expected.getName(), user.getId());

        assertThat(result)
            .isPresent()
            .get()
            .satisfies(userList -> {
                assertThat(userList.getId()).isEqualTo(expected.getId());
                assertThat(userList.getName()).isEqualTo(expected.getName());
                assertThat(userList.getColor()).isEqualTo(expected.getColor());
                assertThat(sameUser(userList.getUser(), user)).isTrue();
            });
    }

    @Test
    void itShouldNotFindByNameWhenUserDoesNotOwnList() {
        var user = getTestUser();
        var otherUserList = getLists().stream()
            .filter(ul -> !sameUser(ul.getUser(), user))
            .findFirst()
            .orElseThrow();

        var result = repository.findByNameAndUserId(otherUserList.getName(), user.getId());

        assertThat(result).isEmpty();
    }
}

