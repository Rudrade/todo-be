package me.rudrade.todo.service;

import me.rudrade.todo.dto.Mapper;
import me.rudrade.todo.dto.TagDto;
import me.rudrade.todo.dto.TaskDto;
import me.rudrade.todo.dto.filter.TaskListFilter;
import me.rudrade.todo.dto.response.TaskListResponse;
import me.rudrade.todo.exception.InvalidAccessException;
import me.rudrade.todo.exception.InvalidDataException;
import me.rudrade.todo.model.Tag;
import me.rudrade.todo.model.Task;
import me.rudrade.todo.model.User;
import me.rudrade.todo.model.UserList;
import me.rudrade.todo.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock private TaskRepository taskRepository;
    @Mock private UserListService userListService;
    @Mock private TagService tagService;
    @Mock private MessageSource messageSource;

    private TaskService taskService;

    private TaskService taskService() {
        if (taskService == null) {
            taskService = new TaskService(userListService, taskRepository, tagService, messageSource);
        }
        return taskService;
    }

    private User user() {
        User user = new User();
        user.setId(UUID.randomUUID());
        return user;
    }

    private Task task(UUID id, String title, String desc, LocalDate dueDate, UserList list, List<Tag> tags) {
        Task task = new Task();
        task.setId(id);
        task.setTitle(title);
        task.setDescription(desc);
        task.setDueDate(dueDate);
        task.setUserList(list);
        task.setTags(tags);
        return task;
    }

    // saveTask
    @Test
    void itShouldSaveNewTaskWithUser() {
        User user = user();
        TaskDto input = new TaskDto(null, "title", "desc", LocalDate.now(), null, null);
        Task saved = Mapper.toTask(input);
        saved.setId(UUID.randomUUID());
        saved.setUser(user);

        when(taskRepository.save(any(Task.class))).thenReturn(saved);

        TaskDto output = taskService().saveTask(input, user);

        assertThat(output.getId()).isEqualTo(saved.getId());
        verify(taskRepository).save(argThat(t -> t.getUser().equals(user)));
        verifyNoInteractions(userListService, tagService);
    }

    @Test
    void itShouldUpdateExistingTaskWhenOwned() {
        User user = user();
        UUID taskId = UUID.randomUUID();
        TaskDto input = new TaskDto(taskId, "title", "desc", LocalDate.now(), null, null);
        Task existing = new Task();
        existing.setId(taskId);

        when(taskRepository.findByIdAndUserId(taskId, user.getId())).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(taskId);
            return t;
        });

        TaskDto output = taskService().saveTask(input, user);

        assertThat(output.getId()).isEqualTo(taskId);
        verify(taskRepository).findByIdAndUserId(taskId, user.getId());
        verify(taskRepository).save(any(Task.class));
        verifyNoMoreInteractions(taskRepository);
        verifyNoInteractions(userListService, tagService);
    }

    @Test
    void itShouldThrowWhenUpdatingNonOwnedTask() {
        User user = user();
        TaskDto input = new TaskDto(UUID.randomUUID(), "title", "desc", LocalDate.now(), null, null);

        when(taskRepository.findByIdAndUserId(input.getId(), user.getId())).thenReturn(Optional.empty());

        TaskService service = taskService();

        assertThrows(InvalidDataException.class, () -> service.saveTask(input, user));

        verify(taskRepository).findByIdAndUserId(input.getId(), user.getId());
        verifyNoMoreInteractions(taskRepository);
        verifyNoInteractions(userListService, tagService);
    }

    @Test
    void itShouldClearListWhenListNameBlank() {
        User user = user();
        TaskDto input = new TaskDto(null, "title", "desc", LocalDate.now(), "   ", null);

        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        TaskDto output = taskService().saveTask(input, user);

        assertThat(output.getListName()).isNull();
        verify(taskRepository).save(argThat(t -> t.getUserList() == null));
        verifyNoInteractions(userListService, tagService);
    }

    @Test
    void itShouldLinkListWhenProvided() {
        User user = user();
        String listName = "Work";
        TaskDto input = new TaskDto(null, "title", "desc", LocalDate.now(), listName, null);
        UserList list = new UserList(UUID.randomUUID(), listName, "red", user, null);

        when(userListService.saveByName(listName, user)).thenReturn(list);
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        TaskDto output = taskService().saveTask(input, user);

        assertThat(output.getListName()).isEqualTo(listName);
        verify(userListService).saveByName(listName, user);
        verify(taskRepository).save(argThat(t -> t.getUserList() != null && listName.equals(t.getUserList().getName())));
        verifyNoMoreInteractions(userListService);
    }

    @Test
    void itShouldCreateTagsWhenProvided() {
        User user = user();
        TagDto tagDto = new TagDto("tag", "color");
        TaskDto input = new TaskDto(null, "title", "desc", LocalDate.now(), null, List.of(tagDto));
        Tag tag = new Tag(UUID.randomUUID(), tagDto.name(), tagDto.color(), user, null);

        when(tagService.findOrCreateByUser(eq(user), any(Tag.class))).thenReturn(tag);
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        TaskDto output = taskService().saveTask(input, user);

        assertThat(output.getTags()).singleElement().isEqualTo(tagDto);
        verify(tagService).findOrCreateByUser(eq(user), any(Tag.class));
        verify(taskRepository).save(argThat(t -> t.getTags() != null && t.getTags().size() == 1));
    }

    // getAll
    @Test
    void itShouldThrowWhenFilterUserInvalid() {
        TaskService service = taskService();
        TaskListFilter nullFilter = null;
        assertThrows(InvalidAccessException.class, () -> service.getAll(nullFilter));

        TaskListFilter userNull = new TaskListFilter(TaskListFilter.Filter.TODAY, null, null);
        assertThrows(InvalidAccessException.class, () -> service.getAll(userNull));

        TaskListFilter userNoId = new TaskListFilter(TaskListFilter.Filter.TODAY, null, new User());
        assertThrows(InvalidAccessException.class, () -> service.getAll(userNoId));

        verifyNoInteractions(taskRepository, userListService, tagService);
    }

    @Test
    void itShouldThrowWhenSearchTermMissingForSearchFilters() {
        User user = user();
        TaskService service = taskService();
        for (TaskListFilter.Filter f : List.of(TaskListFilter.Filter.SEARCH, TaskListFilter.Filter.LIST, TaskListFilter.Filter.TAG)) {
            TaskListFilter filter = new TaskListFilter(f, "   ", user);
            assertThrows(InvalidDataException.class, () -> service.getAll(filter));
        }
        verifyNoInteractions(taskRepository, userListService, tagService);
    }

    @Test
    void itShouldReturnTodayTasks() {
        User user = user();
        TaskListFilter filter = new TaskListFilter(TaskListFilter.Filter.TODAY, null, user);
        Task t1 = task(UUID.randomUUID(), "t1", "d1", LocalDate.now(), null, null);
        Task t2 = task(UUID.randomUUID(), "t2", "d2", LocalDate.now(), null, null);

        when(taskRepository.findDueToday(user.getId(), Pageable.unpaged())).thenReturn(new PageImpl<>(List.of(t1, t2)));

        TaskListResponse response = taskService().getAll(filter);

        assertThat(response.tasks()).containsExactlyInAnyOrder(Mapper.toTaskDto(t1), Mapper.toTaskDto(t2));
        verify(taskRepository).findDueToday(user.getId(), Pageable.unpaged());
        verifyNoMoreInteractions(taskRepository);
    }

    @Test
    void itShouldReturnUpcomingTasks() {
        User user = user();
        TaskListFilter filter = new TaskListFilter(TaskListFilter.Filter.UPCOMING, null, user);
        Task t1 = task(UUID.randomUUID(), "t1", "d1", LocalDate.now().plusDays(1), null, null);
        Task t2 = task(UUID.randomUUID(), "t2", "d2", LocalDate.now().plusDays(2), null, null);

        when(taskRepository.findDueUpcoming(user.getId(), Pageable.unpaged())).thenReturn(new PageImpl<>(List.of(t1, t2)));

        TaskListResponse response = taskService().getAll(filter);

        assertThat(response.tasks()).containsExactlyInAnyOrder(Mapper.toTaskDto(t1), Mapper.toTaskDto(t2));
        verify(taskRepository).findDueUpcoming(user.getId(), Pageable.unpaged());
        verifyNoMoreInteractions(taskRepository);
    }

    @Test
    void itShouldReturnSearchTasks() {
        User user = user();
        TaskListFilter filter = new TaskListFilter(TaskListFilter.Filter.SEARCH, "title", user);
        Task t1 = task(UUID.randomUUID(), "title 1", "d1", LocalDate.now(), null, null);

        when(taskRepository.findByTitleContains("title", user.getId(), Pageable.unpaged())).thenReturn(new PageImpl<>(List.of(t1)));

        TaskListResponse response = taskService().getAll(filter);

        assertThat(response.tasks()).containsExactly(Mapper.toTaskDto(t1));
        verify(taskRepository).findByTitleContains("title", user.getId(), Pageable.unpaged());
        verifyNoMoreInteractions(taskRepository);
    }

    @Test
    void itShouldReturnListTasksWhenListFound() {
        User user = user();
        TaskListFilter filter = new TaskListFilter(TaskListFilter.Filter.LIST, "Work", user);
        Task task = task(UUID.randomUUID(), "t1", "d1", LocalDate.now(), null, null);

        when(taskRepository.findAllByUserListNameAndUserId("Work", user.getId(), Pageable.unpaged())).thenReturn(new PageImpl<>(List.of(task)));

        TaskListResponse response = taskService().getAll(filter);

        assertThat(response.tasks()).containsExactly(Mapper.toTaskDto(task));
        verify(taskRepository, times(1)).findAllByUserListNameAndUserId("Work", user.getId(), Pageable.unpaged());
        verifyNoMoreInteractions(taskRepository);
    }

    @Test
    void itShouldReturnTagTasksWhenTagFound() {
        User user = user();
        TaskListFilter filter = new TaskListFilter(TaskListFilter.Filter.TAG, "tag-1", user);
        Task task = task(UUID.randomUUID(), "t1", "d1", LocalDate.now(), null, null);
        Tag tag = new Tag(UUID.randomUUID(), "tag-1", "color", user, List.of(task));
        task.setTags(List.of(tag));

        when(taskRepository.findAllByTagsNameAndUserId("tag-1", user.getId(), Pageable.unpaged())).thenReturn(new PageImpl<>(List.of(task)));

        TaskListResponse response = taskService().getAll(filter);

        assertThat(response.tasks()).containsExactly(Mapper.toTaskDto(task));
        verify(taskRepository, times(1)).findAllByTagsNameAndUserId("tag-1", user.getId(), Pageable.unpaged());
        verifyNoMoreInteractions(taskRepository);
    }

    @Test
    void itShouldReturnAllByDefault() {
        User user = user();
        TaskListFilter filter = new TaskListFilter(null, null, user);
        Task t1 = task(UUID.randomUUID(), "t1", "d1", LocalDate.now(), null, null);

        when(taskRepository.findAllByUserId(user.getId(), Pageable.unpaged())).thenReturn(new PageImpl<>(List.of(t1)));

        TaskListResponse response = taskService().getAll(filter);

        assertThat(response.tasks()).containsExactly(Mapper.toTaskDto(t1));
        verify(taskRepository).findAllByUserId(user.getId(), Pageable.unpaged());
        verifyNoMoreInteractions(taskRepository);
    }

    // getById
    @Test
    void itShouldGetByIdWhenOwned() {
        User user = user();
        UUID id = UUID.randomUUID();
        Task task = task(id, "t", "d", LocalDate.now(), null, null);

        when(taskRepository.findByIdAndUserId(id, user.getId())).thenReturn(Optional.of(task));

        TaskDto dto = taskService().getById(id, user);

        assertThat(dto).isEqualTo(Mapper.toTaskDto(task));
        verify(taskRepository).findByIdAndUserId(id, user.getId());
    }

    @Test
    void itShouldThrowWhenGetByIdWithInvalidArgs() {
        User userNoId = new User();
        TaskService service = taskService();
        User validUser = user();
        UUID randomId = UUID.randomUUID();

        assertThrows(InvalidAccessException.class, () -> service.getById(null, validUser));
        assertThrows(InvalidAccessException.class, () -> service.getById(randomId, null));
        assertThrows(InvalidAccessException.class, () -> service.getById(randomId, userNoId));
        verifyNoInteractions(taskRepository);
    }

    @Test
    void itShouldThrowWhenTaskNotOwnedOnGetById() {
        User user = user();
        UUID id = UUID.randomUUID();
        when(taskRepository.findByIdAndUserId(id, user.getId())).thenReturn(Optional.empty());

        TaskService service = taskService();

        assertThrows(InvalidDataException.class, () -> service.getById(id, user));

        verify(taskRepository).findByIdAndUserId(id, user.getId());
    }

    // deleteById
    @Test
    void itShouldDeleteWhenOwned() {
        User user = user();
        UUID id = UUID.randomUUID();
        Task task = task(id, "t", "d", LocalDate.now(), null, null);

        when(taskRepository.findByIdAndUserId(id, user.getId())).thenReturn(Optional.of(task));

        TaskService service = taskService();

        service.deleteById(id, user);

        verify(taskRepository).findByIdAndUserId(id, user.getId());
        verify(taskRepository).deleteById(id);
    }

    @Test
    void itShouldThrowWhenDeleteWithInvalidArgs() {
        User userNoId = new User();
        TaskService service = taskService();
        User validUser = user();
        UUID randomId = UUID.randomUUID();

        assertThrows(InvalidAccessException.class, () -> service.deleteById(null, validUser));
        assertThrows(InvalidAccessException.class, () -> service.deleteById(randomId, null));
        assertThrows(InvalidAccessException.class, () -> service.deleteById(randomId, userNoId));
        verifyNoInteractions(taskRepository);
    }

    @Test
    void itShouldThrowWhenDeletingNonOwnedTask() {
        User user = user();
        UUID id = UUID.randomUUID();
        when(taskRepository.findByIdAndUserId(id, user.getId())).thenReturn(Optional.empty());

        TaskService service = taskService();

        assertThrows(InvalidDataException.class, () -> service.deleteById(id, user));

        verify(taskRepository).findByIdAndUserId(id, user.getId());
        verify(taskRepository, never()).deleteById(any());
    }
}