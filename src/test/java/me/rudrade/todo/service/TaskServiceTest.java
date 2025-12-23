package me.rudrade.todo.service;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

import me.rudrade.todo.dto.Mapper;
import me.rudrade.todo.dto.TagDto;
import me.rudrade.todo.dto.TaskDto;
import me.rudrade.todo.dto.filter.TaskListFilter;
import me.rudrade.todo.dto.response.TaskListResponse;
import me.rudrade.todo.exception.TaskNotFoundException;
import me.rudrade.todo.model.Tag;
import me.rudrade.todo.model.Task;
import me.rudrade.todo.model.User;
import me.rudrade.todo.model.UserList;
import me.rudrade.todo.repository.TaskRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock private TaskRepository taskRepository;
    @Mock private UserListService userListService;
    @Mock private TagService tagService;

    private TaskService taskService;

    // ### saveTask ###
    @Test
    void itShouldSaveNewTask() {
        TaskDto input = new TaskDto(null, "title 123", "description 123", LocalDate.now(), null, null);

        Task task = Mapper.toTask(input);
        task.setId(UUID.randomUUID());

        when(taskRepository.save(Mapper.toTask(input)))
                .thenReturn(task);

        TaskDto output = taskService().saveTask(input);
        assertThat(output)
                .isNotNull();
        assertThat(output)
                .usingRecursiveComparison().ignoringFields("id").isEqualTo(input);
        assertThat(output.id()).isNotNull();

        verify(taskRepository, times(1)).save(Mapper.toTask(input));
        verifyNoMoreInteractions(taskRepository);
    }

    @Test
    void itShouldSaveExistingTask() {
        TaskDto input = new TaskDto(UUID.randomUUID(), "title 321", "description 321", LocalDate.now(), null, null);

        Task task = new Task();
        task.setId(input.id());
        when(taskRepository.findById(input.id()))
                .thenReturn(Optional.of(task));

        Task savedTask = Mapper.toTask(input);
        when(taskRepository.save(Mapper.toTask(input)))
                .thenReturn(savedTask);

        TaskDto output = taskService().saveTask(input);
        assertThat(output)
                .isNotNull();
        assertThat(output)
                .usingRecursiveComparison().isEqualTo(input);
        assertThat(output.id()).isNotNull();

        verify(taskRepository, times(1)).findById(input.id());
        verify(taskRepository, times(1)).save(Mapper.toTask(input));
        verifyNoMoreInteractions(taskRepository);
    }

    @Test
    void itShouldNotSaveTaskWithNonExistingId() {
        TaskDto input = new TaskDto(UUID.randomUUID(), "title 123", "description 123", LocalDate.now(), null, null);

        when(taskRepository.findById(input.id()))
                .thenReturn(Optional.empty());


        assertThatThrownBy(() -> {
           taskService().saveTask(input);
        }).isInstanceOf(TaskNotFoundException.class);

        verify(taskRepository, times(1)).findById(input.id());
        verifyNoMoreInteractions(taskRepository);
    }

    @Test
    void itShouldSaveTaskWithEmptyListNameAsNull() {
        TaskDto input = new TaskDto(null, "title", "desc", LocalDate.now(), "", null);
        Task task = Mapper.toTask(input);
        task.setId(UUID.randomUUID());
        task.setUserList(null);

        when(taskRepository.save(any(Task.class))).thenReturn(task);

        TaskDto output = taskService().saveTask(input);

        assertThat(output.listName()).isNull();
        verify(taskRepository).save(argThat(t -> t.getUserList() == null));
        verifyNoInteractions(userListService);
    }

    @Test
    void itShouldSaveTaskAndLinkExistingOrNewList() {
        String listName = "Work";
        TaskDto input = new TaskDto(null, "title", "desc", LocalDate.now(), listName, null);

        UserList mockList = new UserList();
        mockList.setName(listName);

        Task savedTask = Mapper.toTask(input);
        savedTask.setId(UUID.randomUUID());
        savedTask.setUserList(mockList);

        when(userListService.saveByName(eq(listName), any())).thenReturn(mockList);
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        TaskDto output = taskService().saveTask(input);

        assertThat(output.listName()).isEqualTo(listName);
        verify(userListService).saveByName(eq(listName), any());
        verify(taskRepository).save(argThat(t -> t.getUserList() != null && t.getUserList().getName().equals(listName)));
    }
    
    @Test
    void itShouldSaveTagsOnNewTask() {
        Tag tag1 = new Tag(null, "tag-1", "black", null, null);

        Tag tag2 = new Tag(UUID.randomUUID(), "tag-2", "red", null, null);

        List<Tag> tags = new ArrayList<>();
        tags.add(tag1);
        tags.add(tag2);

        User user = new User();
        user.setId(UUID.randomUUID());

        List<TagDto> lsTagsDto = tags.stream().map(Mapper::toTagDto).toList();
        TaskDto input = new TaskDto(null, "title", "desc", LocalDate.now(), null, lsTagsDto);

        when(taskRepository.save(any(Task.class)))
            .thenReturn(Mapper.toTask(input));

        when(tagService.findOrCreateByUser(eq(user), any(Tag.class)))
            .thenAnswer(a -> a.getArguments()[1]);

        TaskDto output = taskService().saveTask(input, user);

        assertThat(output.tags()).isNotNull().containsExactlyInAnyOrderElementsOf(lsTagsDto);

        verify(tagService, times(2)).findOrCreateByUser(eq(user), any(Tag.class));
        verifyNoMoreInteractions(tagService);
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
            null,
            null
        );

        Task task2 = new Task(
                UUID.randomUUID(),
                "title 2",
                "description 2",
                LocalDate.now(),
            null,
            null
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

        List<TaskDto> lstDto = List.of(Mapper.toTaskDto(task1), Mapper.toTaskDto(task2));
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
    void itShouldReturnEmptyListWhenListNotFoundByName() {
        String nonExistentListName = "non-existent-list";
        TaskListFilter filter = new TaskListFilter(TaskListFilter.Filter.LIST, nonExistentListName);

        when(userListService.findByName(nonExistentListName))
            .thenReturn(Optional.empty());

        TaskListResponse output = taskService().getAll(filter);

        assertThat(output).isNotNull();
        assertThat(output.count()).isZero();
        assertThat(output.tasks()).isEmpty();

        verify(userListService, times(1)).findByName(nonExistentListName);
        verifyNoInteractions(taskRepository);
    }

    @Test
    void itShouldGetAllUpcoming() {
        TaskListFilter filter = new TaskListFilter(TaskListFilter.Filter.UPCOMING, null);

        Task task1 = new Task(
                UUID.randomUUID(),
                "title 1",
                "description 1",
                LocalDate.now().plusDays(1),
            null,
            null
        );

        Task task2 = new Task(
                UUID.randomUUID(),
                "title 2",
                "description 2",
                LocalDate.now().plusDays(3),
            null,
            null
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

        List<TaskDto> lstDto = List.of(Mapper.toTaskDto(task1), Mapper.toTaskDto(task2));
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
            null,
            null
        );

        Task task2 = new Task(
                UUID.randomUUID(),
                "title 2",
                "description 2",
                LocalDate.now(),
            null,
            null
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

        List<TaskDto> lstDto = List.of(Mapper.toTaskDto(task1), Mapper.toTaskDto(task2));
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
    void itShouldGetAllByList() {
        TaskListFilter filter = new TaskListFilter(TaskListFilter.Filter.LIST, "test-list");

        UserList lst = new UserList();
        lst.setName("test-list");

        Task task1 = new Task(
            UUID.randomUUID(),
            "title 1",
            "description 1",
            LocalDate.now(),
            lst,
            null
        );

        Task task2 = new Task(
            UUID.randomUUID(),
            "title 2",
            "description 2",
            LocalDate.now(),
            lst,
            null
        );

        lst.setTasks(List.of(task1, task2));

        when(userListService.findByName("test-list"))
            .thenReturn(Optional.of(lst));

        TaskListResponse output = taskService().getAll(filter);
        assertThat(output)
            .isNotNull()
            .hasNoNullFieldsOrProperties();

        assertThat(output.tasks()).hasSize((int) output.count());

        List<TaskDto> lstDto = List.of(Mapper.toTaskDto(task1), Mapper.toTaskDto(task2));
        assertThat(output.tasks())
            .containsExactlyInAnyOrderElementsOf(lstDto);

        assertThat(output.tasks())
            .allSatisfy(arg -> {
                assertThat(arg.listName())
                    .isNotNull()
                    .contains("test-list");
            });

        verifyNoInteractions(taskRepository);
    }

    @Test
    void itShouldGetAll() {
        TaskListFilter filter = new TaskListFilter(null, null);

        Task task1 = new Task(
                UUID.randomUUID(),
                "title 1",
                "description 1",
                LocalDate.now(),
            null,
            null
        );

        Task task2 = new Task(
                UUID.randomUUID(),
                "title 2",
                "description 2",
                LocalDate.now(),
            null,
            null
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

        List<TaskDto> lstDto = List.of(Mapper.toTaskDto(task1), Mapper.toTaskDto(task2));
        assertThat(output.tasks())
                .containsExactlyInAnyOrderElementsOf(lstDto);

        verify(taskRepository, times(1)).findAll();
        verify(taskRepository, times(1)).count();
        verifyNoMoreInteractions(taskRepository);
    }

    @Test
    void itShouldGetAllWithTags() {
        Tag tag1 = new Tag(null, "tag-1", "black", null, null);

        TaskListFilter filter = new TaskListFilter(TaskListFilter.Filter.TAG, "tag-1");

        Task task1 = new Task(UUID.randomUUID(), "title 1", "description 1", LocalDate.now(), null, List.of(tag1));
        Task task2 = new Task(UUID.randomUUID(), "title 2", "description 2", LocalDate.now(), null, List.of(tag1));
        tag1.setTasks(List.of(task1, task2));

        when(tagService.findByName("tag-1")).thenReturn(Optional.of(tag1));

        TaskListResponse output = taskService().getAll(filter);
        assertThat(output)
            .isNotNull();

        assertThat(output.tasks())
            .map(TaskDto::tags)
            .allSatisfy(tags ->
                assertThat(tags).containsExactlyInAnyOrderElementsOf(List.of(Mapper.toTagDto(tag1)))
            );

        verify(tagService, times(1)).findByName("tag-1");
        verifyNoMoreInteractions(tagService);
        verifyNoInteractions(taskRepository);
    }

    // ### getById ###
    @Test
    void itShouldGetById() {
        Task task = new Task();
        task.setId(UUID.randomUUID());
        task.setTitle("title 123");

        when(taskRepository.findById(task.getId()))
                .thenReturn(Optional.of(task));

        TaskDto output = taskService().getById(task.getId());
        assertThat(output)
                .isNotNull()
                .isEqualTo(Mapper.toTaskDto(task));

        verify(taskRepository, times(1))
                .findById(task.getId());
        verifyNoMoreInteractions(taskRepository);
    }

    @Test
    void itShouldThrowWhenNotFoundById() {
        UUID id = UUID.randomUUID();

        when(taskRepository.findById(id))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> {
            taskService().getById(id);
        }).isInstanceOf(TaskNotFoundException.class);

        verify(taskRepository, times(1))
                .findById(id);
        verifyNoMoreInteractions(taskRepository);
    }

    // ### deleteById ###
    @Test
    void itShouldDeleteById() {
        UUID id = UUID.randomUUID();

        when(taskRepository.findById(id))
                .thenReturn(Optional.of(new Task()));

        taskService().deleteById(id);

        verify(taskRepository, times(1))
                .findById(id);
        verify(taskRepository, times(1))
                .deleteById(id);
        verifyNoMoreInteractions(taskRepository);
    }

    @Test
    void itShouldNotDeleteByIdWhenNotFound() {
        UUID id = UUID.randomUUID();

        when(taskRepository.findById(id))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> {
            taskService().deleteById(id);
        }).isInstanceOf(TaskNotFoundException.class);

        verify(taskRepository, times(1))
                .findById(id);
        verifyNoMoreInteractions(taskRepository);
    }

    private TaskService taskService() {
        if (taskService == null) {
            taskService = new TaskService(userListService, taskRepository, tagService);
        }
        return taskService;
    }

}