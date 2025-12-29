package me.rudrade.todo.repository;

import static org.assertj.core.api.Assertions.*;

import me.rudrade.todo.config.SqlIntegrationTest;
import me.rudrade.todo.model.Task;
import me.rudrade.todo.model.User;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.stereotype.Service;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@DataJpaTest(includeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = Service.class))
@Sql("/sql-scripts/INIT_TASKS.sql")
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

        List<Task> result = repository.findDueToday(user.getId());

        LocalDate dtNow = LocalDate.now();
        assertThat(result)
            .isNotEmpty()
            .usingDefaultElementComparator()
            .containsExactlyInAnyOrderElementsOf(
              getAllTasks().stream()
                  .filter(t ->
                      sameUser(t.getUser(), user) &&
                      t.getDueDate() != null &&
                      t.getDueDate().getYear() == dtNow.getYear() &&
                      t.getDueDate().getMonthValue() == dtNow.getMonthValue() &&
                      t.getDueDate().getDayOfMonth() == dtNow.getDayOfMonth()
                  )
                  .toList()
            );
    }

    @Test
    void itShouldFindDueUpcoming() {
        User user = getTestUser();

        List<Task> result = repository.findDueUpcoming(user.getId());

        LocalDate dtNow = LocalDate.now();
        assertThat(result)
            .isNotEmpty()
            .usingDefaultElementComparator()
            .containsExactlyInAnyOrderElementsOf(
                getAllTasks().stream()
                    .filter(t ->
                        sameUser(t.getUser(), user) &&
                        t.getDueDate() != null &&
                        t.getDueDate().isAfter(dtNow)
                    ).toList()
            );
    }

    @Test
    void itShouldFindByTitleContains() {
        User user = getTestUser();

        List<Task> result = repository.findByTitleContains("toDaY", user.getId());

        assertThat(result)
            .isNotEmpty()
            .containsExactlyInAnyOrderElementsOf(
                getAllTasks().stream()
                    .filter(t -> 
                        sameUser(t.getUser(), user) &&
                        t.getTitle() != null &&
                        t.getTitle().toLowerCase().contains("today")
                    ).toList()
            );
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

        List<Task> result = repository.findAllByUserId(user.getId());

        assertThat(result)
            .isNotEmpty()
            .usingDefaultElementComparator()
            .containsExactlyInAnyOrderElementsOf(
                getAllTasks().stream()
                    .filter(t -> sameUser(t.getUser(), user))
                    .toList()
            );
    }

}
