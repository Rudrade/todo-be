package me.rudrade.todo.service;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

import me.rudrade.todo.dto.Mapper;
import me.rudrade.todo.dto.TaskDto;
import me.rudrade.todo.dto.filter.TaskListFilter;
import me.rudrade.todo.dto.response.TaskListResponse;
import me.rudrade.todo.exception.TaskNotFoundException;
import me.rudrade.todo.model.Task;
import me.rudrade.todo.repository.TaskRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock private TaskRepository taskRepository;

    private final Mapper mapper = new Mapper();
    private TaskService taskService;

    // ### saveTask ###
    @Test
    void itShouldSaveNewTask() {
        TaskDto input = new TaskDto(null, "title 123", "description 123", LocalDate.now(), true);

        Task task = mapper.toTask(input);
        task.setId(UUID.randomUUID());

        when(taskRepository.save(mapper.toTask(input)))
                .thenReturn(task);

        TaskDto output = taskService().saveTask(input);
        assertThat(output)
                .isNotNull();
        assertThat(output)
                .usingRecursiveComparison().ignoringFields("id").isEqualTo(input);
        assertThat(output.id()).isNotNull();

        verify(taskRepository, times(1)).save(mapper.toTask(input));
        verifyNoMoreInteractions(taskRepository);
    }

    @Test
    void itShouldSaveExistingTask() {
        TaskDto input = new TaskDto(UUID.randomUUID(), "title 321", "description 321", LocalDate.now(), true);

        Task task = new Task();
        task.setId(input.id());
        when(taskRepository.findById(input.id()))
                .thenReturn(Optional.of(task));

        Task savedTask = mapper.toTask(input);
        when(taskRepository.save(mapper.toTask(input)))
                .thenReturn(savedTask);

        TaskDto output = taskService().saveTask(input);
        assertThat(output)
                .isNotNull();
        assertThat(output)
                .usingRecursiveComparison().isEqualTo(input);
        assertThat(output.id()).isNotNull();

        verify(taskRepository, times(1)).findById(input.id());
        verify(taskRepository, times(1)).save(mapper.toTask(input));
        verifyNoMoreInteractions(taskRepository);
    }

    @Test
    void itShouldNotSaveTaskWithNonExistingId() {
        TaskDto input = new TaskDto(UUID.randomUUID(), "title 123", "description 123", LocalDate.now(), true);

        when(taskRepository.findById(input.id()))
                .thenReturn(Optional.empty());


        assertThatThrownBy(() -> {
           taskService().saveTask(input);
        }).isInstanceOf(TaskNotFoundException.class);

        verify(taskRepository, times(1)).findById(input.id());
        verifyNoMoreInteractions(taskRepository);
    }

    // ### getAll ###
    @Test
    void itShouldGetAllToday() {
        TaskListFilter filter = new TaskListFilter(TaskListFilter.Filter.TODAY, null);

        Task task1 = new Task(
                UUID.randomUUID(),
                "title 1",
                "description 1",
                LocalDate.now(),
                false
        );

        Task task2 = new Task(
                UUID.randomUUID(),
                "title 2",
                "description 2",
                LocalDate.now(),
                false
        );

        when(taskRepository.findDueToday())
                .thenReturn(List.of(task1, task2));

        when(taskRepository.countFindDueToday())
                .thenReturn(2L);

        TaskListResponse output = taskService().getAll(filter);
        assertThat(output)
                .isNotNull()
                .hasNoNullFieldsOrProperties();

        assertThat(output.tasks()).hasSize((int) output.count());

        List<TaskDto> lstDto = List.of(mapper.toTaskDto(task1), mapper.toTaskDto(task2));
        assertThat(output.tasks())
                .containsExactlyInAnyOrderElementsOf(lstDto);

        assertThat(output.tasks())
                .allSatisfy(arg -> {
                    assertThat(arg.dueDate())
                            .isNotNull()
                            .isToday();
                });

        verify(taskRepository, times(1)).findDueToday();
        verify(taskRepository, times(1)).countFindDueToday();
        verifyNoMoreInteractions(taskRepository);
    }

    @Test
    void itShouldGetAllUpcoming() {
        TaskListFilter filter = new TaskListFilter(TaskListFilter.Filter.UPCOMING, null);

        Task task1 = new Task(
                UUID.randomUUID(),
                "title 1",
                "description 1",
                LocalDate.now().plusDays(1),
                false
        );

        Task task2 = new Task(
                UUID.randomUUID(),
                "title 2",
                "description 2",
                LocalDate.now().plusDays(3),
                false
        );

        when(taskRepository.findDueUpcoming())
                .thenReturn(List.of(task1, task2));

        when(taskRepository.countFindDueUpcoming())
                .thenReturn(2L);

        TaskListResponse output = taskService().getAll(filter);
        assertThat(output)
                .isNotNull()
                .hasNoNullFieldsOrProperties();

        assertThat(output.tasks()).hasSize((int) output.count());

        List<TaskDto> lstDto = List.of(mapper.toTaskDto(task1), mapper.toTaskDto(task2));
        assertThat(output.tasks())
                .containsExactlyInAnyOrderElementsOf(lstDto);

        assertThat(output.tasks())
                .allSatisfy(arg -> {
                    assertThat(arg.dueDate())
                            .isNotNull()
                            .isInTheFuture();
                });

        verify(taskRepository, times(1)).findDueUpcoming();
        verify(taskRepository, times(1)).countFindDueUpcoming();
        verifyNoMoreInteractions(taskRepository);
    }

    @Test
    void itShouldGetAllByTitle() {
        TaskListFilter filter = new TaskListFilter(TaskListFilter.Filter.SEARCH, "title");

        Task task1 = new Task(
                UUID.randomUUID(),
                "title 1",
                "description 1",
                LocalDate.now(),
                false
        );

        Task task2 = new Task(
                UUID.randomUUID(),
                "title 2",
                "description 2",
                LocalDate.now(),
                false
        );

        when(taskRepository.findByTitleContains("title"))
                .thenReturn(List.of(task1, task2));

        when(taskRepository.countByTitleContains("title"))
                .thenReturn(2L);

        TaskListResponse output = taskService().getAll(filter);
        assertThat(output)
                .isNotNull()
                .hasNoNullFieldsOrProperties();

        assertThat(output.tasks()).hasSize((int) output.count());

        List<TaskDto> lstDto = List.of(mapper.toTaskDto(task1), mapper.toTaskDto(task2));
        assertThat(output.tasks())
                .containsExactlyInAnyOrderElementsOf(lstDto);

        assertThat(output.tasks())
                .allSatisfy(arg -> {
                    assertThat(arg.title())
                            .isNotNull()
                            .contains("title");
                });

        verify(taskRepository, times(1)).findByTitleContains("title");
        verify(taskRepository, times(1)).countByTitleContains("title");
        verifyNoMoreInteractions(taskRepository);
    }

    @Test
    void itShouldGetAll() {
        TaskListFilter filter = new TaskListFilter(null, null);

        Task task1 = new Task(
                UUID.randomUUID(),
                "title 1",
                "description 1",
                LocalDate.now(),
                false
        );

        Task task2 = new Task(
                UUID.randomUUID(),
                "title 2",
                "description 2",
                LocalDate.now(),
                false
        );

        when(taskRepository.findAll())
                .thenReturn(List.of(task1, task2));

        when(taskRepository.count())
                .thenReturn(2L);

        TaskListResponse output = taskService().getAll(filter);
        assertThat(output)
                .isNotNull()
                .hasNoNullFieldsOrProperties();

        assertThat(output.tasks()).hasSize((int) output.count());

        List<TaskDto> lstDto = List.of(mapper.toTaskDto(task1), mapper.toTaskDto(task2));
        assertThat(output.tasks())
                .containsExactlyInAnyOrderElementsOf(lstDto);

        verify(taskRepository, times(1)).findAll();
        verify(taskRepository, times(1)).count();
        verifyNoMoreInteractions(taskRepository);
    }

    private TaskService taskService() {
        if (taskService == null) {
            taskService = new TaskService(taskRepository, mapper);
        }
        return taskService;
    }

}