package me.rudrade.todo.repository;

import me.rudrade.todo.config.ConfigurationUtil;
import me.rudrade.todo.config.SqlIntegrationTest;
import me.rudrade.todo.model.Tag;
import me.rudrade.todo.model.User;
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
@Sql("/sql-scripts/INIT_TAGS.sql")
@Import(ConfigurationUtil.class)
class TagRepositoryTest extends SqlIntegrationTest {

    @Autowired private TagRepository repository;

    private List<Tag> lstTags = null;
    private List<Tag> getTags() {
        if (lstTags == null) {
            lstTags = new ArrayList<>();
            repository.findAll().forEach(lstTags::add);
            assertThat(lstTags).as("No tags present in DB.").isNotEmpty();
        }

        return lstTags;
    }

    @Test
    void itShouldFindByUserId() {
        User user = getTestUser();

        List<Tag> result = repository.findByUserId(user.getId());

        assertThat(result)
            .isNotEmpty()
            .containsExactlyInAnyOrderElementsOf(
                getTags().stream()
                .filter(t -> sameUser(t.getUser(), user))
                .toList());
    }

    @Test
    void itShouldFindByNameAndUserId() {
        User user = getTestUser();
        Tag expected = getTags().stream()
            .filter(t -> sameUser(t.getUser(), user))
            .findFirst()
            .orElseThrow();

        var result = repository.findByNameAndUserId(expected.getName(), user.getId());

        assertThat(result)
            .isPresent()
            .get()
            .satisfies(tag -> {
                assertThat(tag.getId()).isEqualTo(expected.getId());
                assertThat(tag.getName()).isEqualTo(expected.getName());
                assertThat(tag.getColor()).isEqualTo(expected.getColor());
                assertThat(sameUser(tag.getUser(), user)).isTrue();
            });
    }

    @Test
    void itShouldNotFindByNameWhenUserDoesNotOwnTag() {
        User user = getTestUser();
        Tag otherUserTag = getTags().stream()
            .filter(t -> !sameUser(t.getUser(), user))
            .findFirst()
            .orElseThrow();

        var result = repository.findByNameAndUserId(otherUserTag.getName(), user.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void itShouldFindByIdAndUserId() {
        User user = getTestUser();
        Tag expected = getTags().stream()
            .filter(t -> sameUser(t.getUser(), user))
            .findFirst()
            .orElseThrow();

        var result = repository.findByIdAndUserId(expected.getId(), user.getId());

        assertThat(result)
            .isPresent()
            .get()
            .satisfies(tag -> {
                assertThat(tag.getId()).isEqualTo(expected.getId());
                assertThat(tag.getName()).isEqualTo(expected.getName());
                assertThat(tag.getColor()).isEqualTo(expected.getColor());
                assertThat(sameUser(tag.getUser(), user)).isTrue();
            });
    }

    @Test
    void itShouldNotFindByIdWhenUserDoesNotOwnTag() {
        User user = getTestUser();
        Tag otherUserTag = getTags().stream()
            .filter(t -> !sameUser(t.getUser(), user))
            .findFirst()
            .orElseThrow();

        var result = repository.findByIdAndUserId(otherUserTag.getId(), user.getId());

        assertThat(result).isEmpty();
    }

}
