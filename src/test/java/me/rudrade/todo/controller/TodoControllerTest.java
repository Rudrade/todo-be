package me.rudrade.todo.controller;

import me.rudrade.todo.config.ControllerIntegrationTest;
import me.rudrade.todo.dto.TaskDto;
import me.rudrade.todo.dto.UserListDto;
import me.rudrade.todo.dto.filter.TaskListFilter;
import me.rudrade.todo.dto.response.TaskListResponse;
import me.rudrade.todo.dto.response.UserListResponse;
import me.rudrade.todo.exception.TaskNotFoundException;
import me.rudrade.todo.model.User;
import me.rudrade.todo.model.UserList;
import me.rudrade.todo.repository.UserListRepository;
import me.rudrade.todo.repository.UserRepository;
import me.rudrade.todo.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@Sql({"/sql-scripts/INIT_USERS.sql", "/sql-scripts/INIT_TASKS.sql"})
class TodoControllerTest extends ControllerIntegrationTest {

    private static final String URI_GET_ALL = "/todo/api/task";
    private static final String URI_SAVE_TASK = "/todo/api/task/save";
    private static final String URI_GET_DETAIL = "/todo/api/task/detail/{id}";
    private static final String URI_DELETE = "/todo/api/task/remove/{id}";
    private static final String URI_GET_LISTS= "/todo/api/task/lists";

    @Autowired private TaskService taskService;
    @Autowired private UserRepository userRepository;
    @Autowired private UserListRepository listRepository;

    @Autowired private MockMvcTester mvc;

    @Test
    void itShouldSaveTask() throws Exception {
        TaskDto input = new TaskDto(
            null,
            "title insert",
            "description new",
            LocalDate.now(),
            null,
            null
        );

        assertThat(mvc.post().uri(URI_SAVE_TASK)
            .contentType(MediaType.APPLICATION_JSON)
            .headers(getAuthHeader())
            .content(mapper().writeValueAsString(input))
        )
        .hasStatusOk()
        .bodyJson().convertTo(TaskDto.class)
        .satisfies(response -> {
            assertThat(response)
                .usingRecursiveComparison().ignoringFields("id")
                .isEqualTo(input);
            assertThat(response.id()).isNotNull();
        });
    }

    @Test
    void itShouldReturnBadRequestWhenSaveTaskNotFound() throws Exception {
        TaskDto input = new TaskDto(
            UUID.randomUUID(),
            "title insert",
            "description new",
            LocalDate.now(),
            null,
            null
        );

        assertThat(mvc.post().uri(URI_SAVE_TASK)
            .contentType(MediaType.APPLICATION_JSON)
            .headers(getAuthHeader())
            .content(mapper().writeValueAsString(input))
        )
        .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void itShouldGetAll() throws Exception {
        assertThat(mvc.get().uri(URI_GET_ALL)
            .headers(getAuthHeader())
        ).hasStatusOk()
        .bodyJson().convertTo(TaskListResponse.class)
        .satisfies(response -> {
            assertThat(response.tasks()).isNotEmpty();
            assertThat(response.count()).isPositive();
        });
    }

    @Test
    void itShouldGetAllWithFilter() throws Exception {
        assertThat(mvc.get().uri(URI_GET_ALL)
            .headers(getAuthHeader())
            .queryParam("filter", TaskListFilter.Filter.TODAY.name())
        ).hasStatusOk()
        .bodyJson().convertTo(TaskListResponse.class)
        .satisfies(response -> {
            assertThat(response.tasks())
                .isNotEmpty()
                .allSatisfy(task -> {
                    assertThat(task.dueDate())
                        .isNotNull()
                        .isToday();
                });
            assertThat(response.count()).isPositive();
        });
    }

    @Test
    void itShouldGetAllWithFilterAndSearchTerm() throws Exception {
        assertThat(mvc.get().uri(URI_GET_ALL)
            .headers(getAuthHeader())
            .queryParam("filter", TaskListFilter.Filter.SEARCH.name())
            .queryParam("searchTerm", "abc")
        ).hasStatusOk()
        .bodyJson().convertTo(TaskListResponse.class)
        .satisfies(response -> {
            assertThat(response.tasks())
                .isNotEmpty()
                .allSatisfy(task -> {
                    assertThat(task.title())
                        .containsIgnoringCase("abc");
                });
            assertThat(response.count()).isPositive();
        });
    }

    @Test
    void itShouldGetDetail() throws Exception {
        TaskDto input = new TaskDto(
            null,
            "title insert",
            "description new",
            LocalDate.now(),
            null,
            null
        );
        TaskDto savedTask = taskService.saveTask(input, null);

        assertThat(mvc.get().uri(URI_GET_DETAIL, savedTask.id().toString())
            .headers(getAuthHeader())
        ).hasStatusOk()
            .bodyJson().convertTo(TaskDto.class)
            .isEqualTo(savedTask);
    }

    @Test
    void itShouldNotGetDetailWhenDoesntExist() throws Exception {
        assertThat(mvc.get().uri(URI_GET_DETAIL, UUID.randomUUID().toString())
            .headers(getAuthHeader())
        ).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void itShouldDeleteById() throws Exception {
        TaskDto input = new TaskDto(
            null,
            "title insert",
            "description new",
            LocalDate.now(),
            null,
            null
        );
        TaskDto savedTask = taskService.saveTask(input, null);

        assertThat(mvc.delete().uri(URI_DELETE, savedTask.id().toString())
            .headers(getAuthHeader())
        ).hasStatusOk();

        assertThatThrownBy(() -> taskService.getById(savedTask.id()))
            .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void itShouldReturnErrorWhenDeleteDoesntExist() throws Exception {
        assertThat(mvc.delete().uri(URI_DELETE, UUID.randomUUID().toString())
            .headers(getAuthHeader())
        ).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void itShouldReturnUserLists() throws Exception {
        Optional<User> user = userRepository.findByUsername("valid-user");
        if (user.isEmpty()) {
            fail("Missing user");
            return;
        }

        UserList list1 = new UserList();
        list1.setName("List One");
        list1.setUser(user.get());
        list1.setColor("random-color");
        listRepository.save(list1);

        UserList list2 = new UserList();
        list2.setName("Second AGB");
        list2.setUser(user.get());
        list2.setColor("random-color");
        listRepository.save(list2);

        assertThat(mvc.get().uri(URI_GET_LISTS).headers(getAuthHeader()))
            .hasStatusOk()
            .bodyJson()
            .convertTo(UserListResponse.class)
            .satisfies(response -> {
                assertThat(response.lists())
                    .map(UserListDto::name)
                    .containsExactlyInAnyOrder("List One", "Second AGB");
            });
    }


}
