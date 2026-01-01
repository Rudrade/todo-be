package me.rudrade.todo.repository;

import static org.assertj.core.api.Assertions.*;

import me.rudrade.todo.config.ConfigurationUtil;
import me.rudrade.todo.config.SqlIntegrationTest;
import me.rudrade.todo.model.Task;
import me.rudrade.todo.model.User;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@DataJpaTest(includeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = Service.class))
@Sql("/sql-scripts/INIT_TASKS.sql")
@Import(ConfigurationUtil.class)
class TaskRepositoryTest extends SqlIntegrationTest  {

    @Autowired private TaskRepository repository;

    private List<Task> getAllTasks() {
        List<Task> lst = new ArrayList<>();
        repository.findAll().forEach(lst::add);
        return lst;
    }

    @Test
    void itShouldFindDueToday() {
        User user = getTestUser();

        var result = repository.findDueToday(user.getId(), Pageable.unpaged());

        LocalDate dtNow = LocalDate.now();
        assertThat(result).isNotEmpty();

        var expected = getAllTasks().stream()
                  .filter(t ->
                      sameUser(t.getUser(), user) &&
                      t.getDueDate() != null &&
                      t.getDueDate().getYear() == dtNow.getYear() &&
                      t.getDueDate().getMonthValue() == dtNow.getMonthValue() &&
                      t.getDueDate().getDayOfMonth() == dtNow.getDayOfMonth()
                  )
                  .toList();

        assertThat(result.getContent())
            .usingDefaultElementComparator()
            .containsExactlyInAnyOrderElementsOf(expected);

        assertThat(result.getTotalElements()).isEqualTo(expected.size());
    }

    @Test
    void itShouldFindDueUpcoming() {
        User user = getTestUser();

        var result = repository.findDueUpcoming(user.getId(), Pageable.unpaged());

        LocalDate dtNow = LocalDate.now();
        assertThat(result).isNotEmpty();
            
        var expected = getAllTasks().stream()
                    .filter(t ->
                        sameUser(t.getUser(), user) &&
                        t.getDueDate() != null &&
                        t.getDueDate().isAfter(dtNow)
                    ).toList();
        
        assertThat(result.getContent())
            .usingDefaultElementComparator()
            .containsExactlyInAnyOrderElementsOf(expected);

        assertThat(result.getTotalElements()).isEqualTo(expected.size());
    }

    @Test
    void itShouldFindByTitleContains() {
        User user = getTestUser();

        var result = repository.findByTitleContains("toDaY", user.getId(), Pageable.unpaged());

        assertThat(result).isNotEmpty();

        var expected = getAllTasks().stream()
                    .filter(t -> 
                        sameUser(t.getUser(), user) &&
                        t.getTitle() != null &&
                        t.getTitle().toLowerCase().contains("today")
                    ).toList();

        assertThat(result.getContent())
            .containsExactlyInAnyOrderElementsOf(expected);

        assertThat(result.getTotalElements()).isEqualTo(expected.size());
    }

    @Test
    void itShouldFindByIdAndUserId() {
        User user = getTestUser();
        Task expected = getAllTasks().stream()
            .filter(t -> sameUser(t.getUser(), user))
            .findFirst()
            .orElseThrow();

        var result = repository.findByIdAndUserId(expected.getId(), user.getId());

        assertThat(result)
            .isPresent()
            .get()
            .satisfies(task -> {
                assertThat(task.getId()).isEqualTo(expected.getId());
                assertThat(task.getTitle()).isEqualTo(expected.getTitle());
                assertThat(task.getDescription()).isEqualTo(expected.getDescription());
                assertThat(task.getDueDate()).isEqualTo(expected.getDueDate());
                assertThat(sameUser(task.getUser(), user)).isTrue();
            });
    }

    @Test
    void itShouldNotFindByIdWhenUserDoesNotOwnTask() {
        User user = getTestUser();
        Task otherUserTask = getAllTasks().stream()
            .filter(t -> !sameUser(t.getUser(), user))
            .findFirst()
            .orElseThrow();

        var result = repository.findByIdAndUserId(otherUserTask.getId(), user.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void itShouldFindAllByUserId() {
        User user = getTestUser();

        var result = repository.findAllByUserId(user.getId(), Pageable.unpaged());

        assertThat(result)
            .isNotEmpty();

        var expected = getAllTasks().stream()
                .filter(t -> sameUser(t.getUser(), user))
                .toList();

        assertThat(result.getContent())
            .usingDefaultElementComparator()
            .containsExactlyInAnyOrderElementsOf(expected);

        assertThat(result.getTotalElements()).isEqualTo(expected.size());
    }

    @Test
    void itShouldFindAllByUserListNameAndUserId() {
        User user = getTestUser();
        
        var targetList = "test-list";

        var expected = getAllTasks().stream()
                .filter(t -> t.getUserList() != null)
                .filter(t -> t.getUserList().getName().equals(targetList))
                .filter(t -> sameUser(t.getUser(), user))
                .toList();

        var result = repository.findAllByUserListNameAndUserId(targetList, user.getId(), Pageable.unpaged());

        assertThat(result)
            .isNotNull();
        assertThat(result.getContent())
            .usingDefaultElementComparator()
            .containsExactlyInAnyOrderElementsOf(expected);
        assertThat(result.getTotalElements()).isEqualTo(expected.size());
    }

    @Test
    void itShouldFindAllByTagsNameAndUserId() {
        var user = getTestUser();
        var tag = "test-tag";

        var expected = getAllTasks().stream()
                .filter(t -> sameUser(t.getUser(), user))
                .filter(t -> t.getTags() != null)
                .filter(t -> t.getTags().stream().anyMatch(tg -> tag.equals(tg.getName())))
                .toList();

        var result = repository.findAllByTagsNameAndUserId(tag, user.getId(), Pageable.unpaged());

        assertThat(result)
            .isNotNull();
        assertThat(result.getContent())
            .usingDefaultElementComparator()
            .containsExactlyInAnyOrderElementsOf(expected);
        assertThat(result.getTotalElements()).isEqualTo(expected.size());
    }

}
