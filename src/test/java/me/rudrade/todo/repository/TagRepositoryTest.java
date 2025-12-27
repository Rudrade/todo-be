package me.rudrade.todo.repository;

import me.rudrade.todo.config.SqlIntegrationTest;
import me.rudrade.todo.model.Tag;
import me.rudrade.todo.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest(includeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = Service.class))
class TagRepositoryTest extends SqlIntegrationTest {

    @Autowired private TagRepository repository;

    @Test
    void itShouldFindByUserId() {
        User user = getTestUser();

        Tag tag = new Tag();
        tag.setName("test-tag");
        tag.setColor("red");
        tag.setUser(user);
        repository.save(tag);

        Tag tag2 = new Tag();
        tag2.setName("test-tag-2");
        tag2.setColor("blue");
        tag2.setUser(user);
        repository.save(tag2);

        List<Tag> result = repository.findByUserId(user.getId());

        assertThat(result)
            .usingElementComparator(Comparator.comparing(Tag::getId).thenComparing(Tag::getColor).thenComparing(Tag::getName))
            .containsExactlyInAnyOrderElementsOf(List.of(tag, tag2));
    }

}
