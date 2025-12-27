package me.rudrade.todo.repository;

import static org.assertj.core.api.Assertions.*;

import me.rudrade.todo.config.SqlIntegrationTest;
import me.rudrade.todo.model.Task;
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
        List<Task> result = repository.findDueToday();

        LocalDate dtNow = LocalDate.now();
        assertThat(result)
            .isNotEmpty()
            .usingDefaultElementComparator()
            .containsExactlyInAnyOrderElementsOf(
              getAllTasks().stream()
                  .filter(t ->
                      t.getDueDate() != null &&
                      t.getDueDate().getYear() == dtNow.getYear() &&
                      t.getDueDate().getMonthValue() == dtNow.getMonthValue() &&
                      t.getDueDate().getDayOfMonth() == dtNow.getDayOfMonth()
                  )
                  .toList()
            );
    }

    @Test
    void itShouldCountDueToday() {
        long result = repository.countFindDueToday();

        LocalDate dtNow = LocalDate.now();
        assertThat(result)
            .isPositive()
            .isEqualTo(
                getAllTasks().stream()
                    .filter(t ->
                        t.getDueDate() != null &&
                        t.getDueDate().getYear() == dtNow.getYear() &&
                        t.getDueDate().getMonthValue() == dtNow.getMonthValue() &&
                        t.getDueDate().getDayOfMonth() == dtNow.getDayOfMonth()
                    )
                    .count()
            );
    }

    @Test
    void itShouldFindDueUpcoming() {
        List<Task> result = repository.findDueUpcoming();

        LocalDate dtNow = LocalDate.now();
        assertThat(result)
            .isNotEmpty()
            .usingDefaultElementComparator()
            .containsExactlyInAnyOrderElementsOf(
                getAllTasks().stream()
                    .filter(t -> t.getDueDate() != null && t.getDueDate().isAfter(dtNow))
                    .toList()
            );
    }

    @Test
    void itShouldCountDueUpcoming() {
        long result = repository.countFindDueUpcoming();

        LocalDate dtNow = LocalDate.now();
        assertThat(result)
            .isPositive()
            .isEqualTo(getAllTasks().stream()
                .filter(t -> t.getDueDate() != null && t.getDueDate().isAfter(dtNow))
                .count()
            );
    }

    @Test
    void itShouldFindByTitleContains() {
        List<Task> result = repository.findByTitleContains("abc");

        assertThat(result)
            .isNotEmpty()
            .containsExactlyInAnyOrderElementsOf(
                getAllTasks().stream()
                    .filter(t -> t.getTitle() != null && t.getTitle().toLowerCase().contains("abc"))
                    .toList()
            );
    }

    @Test
    void itShouldCountByTitleContains() {
        long result = repository.countByTitleContains("AbC");

        assertThat(result)
            .isPositive()
            .isEqualTo(
                getAllTasks().stream()
                    .filter(t -> t.getTitle() != null && t.getTitle().toLowerCase().contains("abc"))
                    .count()
            );
    }
}
