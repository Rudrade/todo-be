package me.rudrade.todo.controller;

import me.rudrade.todo.config.ControllerIntegration;
import me.rudrade.todo.dto.TaskDto;
import me.rudrade.todo.dto.UserListDto;
import me.rudrade.todo.dto.filter.TaskListFilter;
import me.rudrade.todo.dto.response.TaskListResponse;
import me.rudrade.todo.dto.response.UserListResponse;
import me.rudrade.todo.exception.InvalidDataException;
import me.rudrade.todo.model.User;
import me.rudrade.todo.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Sql(scripts = {"/sql-scripts/INIT_TASKS.sql"}, executionPhase = ExecutionPhase.BEFORE_TEST_CLASS)
class TodoControllerTest extends ControllerIntegration {

    private static final String URI_GET_ALL = "/task";
    private static final String URI_SAVE_TASK = "/task/save";
    private static final String URI_GET_DETAIL = "/task/detail/{id}";
    private static final String URI_DELETE = "/task/remove/{id}";
    private static final String URI_GET_LISTS= "/task/lists";

    @Autowired private TaskService taskService;

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

        var request = mvc.post().uri(URI_SAVE_TASK)
            .contentType(MediaType.APPLICATION_JSON)
            .headers(getAuthHeader())
            .content(mapper().writeValueAsString(input));

        assertThat(request)
            .hasStatusOk()
            .bodyJson().convertTo(TaskDto.class)
            .satisfies(response -> {
                assertThat(response)
                    .usingRecursiveComparison().ignoringFields("id")
                    .isEqualTo(input);
                assertThat(response.getId()).isNotNull();
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
    void itShouldGetAll() {
        assertThat(mvc.get().uri(URI_GET_ALL)
            .headers(getAuthHeader())
        ).hasStatusOk()
        .bodyJson().convertTo(TaskListResponse.class)
        .satisfies(response -> {
            assertThat(response.tasks()).isNotEmpty();
        });
    }

    @Test
    void itShouldGetAllWithFilter() {
        assertThat(mvc.get().uri(URI_GET_ALL)
            .headers(getAuthHeader())
            .queryParam("filter", TaskListFilter.Filter.TODAY.name())
        ).hasStatusOk()
        .bodyJson().convertTo(TaskListResponse.class)
        .satisfies(response -> {
            assertThat(response.tasks())
                .isNotEmpty()
                .allSatisfy(task -> {
                    assertThat(task.getDueDate())
                        .isNotNull()
                        .isToday();
                });
        });
    }

    @Test
    void itShouldGetAllWithFilterAndSearchTerm() {
        assertThat(mvc.get().uri(URI_GET_ALL)
            .headers(getAuthHeader())
            .queryParam("filter", TaskListFilter.Filter.SEARCH.name())
            .queryParam("searchTerm", "title")
        ).hasStatusOk()
        .bodyJson().convertTo(TaskListResponse.class)
        .satisfies(response -> {
            assertThat(response.tasks())
                .isNotEmpty()
                .allSatisfy(task -> {
                    assertThat(task.getTitle())
                        .containsIgnoringCase("title");
                });
        });
    }

    @Test
    void itShouldGetDetail() {
        TaskDto input = new TaskDto(
            null,
            "title insert",
            "description new",
            LocalDate.now(),
            null,
            null
        );
        TaskDto savedTask = taskService.saveTask(input, getTestUser());

        var assertResp = assertThat(mvc.get().uri(URI_GET_DETAIL, savedTask.getId().toString())
            .headers(getAuthHeader()));

        assertResp.hasStatusOk()
            .bodyJson()
            .convertTo(TaskDto.class)
            .isEqualTo(savedTask);
    }

    @Test
    void itShouldNotGetDetailWhenDoesntExist() {
        assertThat(mvc.get().uri(URI_GET_DETAIL, UUID.randomUUID().toString())
            .headers(getAuthHeader())
        ).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void itShouldDeleteById() {
        TaskDto input = new TaskDto(
            null,
            "title insert",
            "description new",
            LocalDate.now(),
            null,
            null
        );

        User user = getTestUser();

        TaskDto savedTask = taskService.saveTask(input, user);

        assertThat(mvc.delete().uri(URI_DELETE, savedTask.getId().toString())
            .headers(getAuthHeader())
        ).hasStatusOk();

        TaskService service = taskService;
        UUID deletedId = savedTask.getId();
        assertThrows(InvalidDataException.class, () -> service.getById(deletedId, user));
    }

    @Test
    void itShouldReturnErrorWhenDeleteDoesntExist() {
        assertThat(mvc.delete().uri(URI_DELETE, UUID.randomUUID().toString())
            .headers(getAuthHeader())
        ).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void itShouldReturnUserLists() {
        assertThat(mvc.get().uri(URI_GET_LISTS).headers(getAuthHeader()))
            .hasStatusOk()
            .bodyJson()
            .convertTo(UserListResponse.class)
            .satisfies(response -> {
                assertThat(response.lists())
                    .map(UserListDto::name)
                    .containsExactlyInAnyOrder("test-list-2", "test-list");
            });
    }

}
