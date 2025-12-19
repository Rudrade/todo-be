package me.rudrade.todo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.rudrade.todo.config.SqlIntegrationTest;
import me.rudrade.todo.dto.TaskDto;
import me.rudrade.todo.dto.UserDto;
import me.rudrade.todo.dto.filter.TaskListFilter;
import me.rudrade.todo.dto.response.LoginResponse;
import me.rudrade.todo.dto.response.TaskListResponse;
import me.rudrade.todo.exception.TaskNotFoundException;
import me.rudrade.todo.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureWebTestClient
@Sql({"/sql-scripts/INIT_USERS.sql", "/sql-scripts/INIT_TASKS.sql"})
class TodoControllerTest extends SqlIntegrationTest {

    private static final String URI_GET_ALL = "/todo/api/task";
    private static final String URI_SAVE_TASK = "/todo/api/task/save";
    private static final String URI_GET_DETAIL = "/todo/api/task/detail/{id}";
    private static final String URI_DELETE = "/todo/api/task/remove/{id}";

    private final ObjectMapper mapper = new ObjectMapper();
    {
        mapper.findAndRegisterModules();
    }

    @Autowired MockMvcTester mvc;
    @Autowired TaskService taskService;

    @Test
    void itShouldSaveTask() throws Exception {
        TaskDto input = new TaskDto(
            null,
            "title insert",
            "description new",
            LocalDate.now()
        );

        assertThat(mvc.post().uri(URI_SAVE_TASK)
            .contentType(MediaType.APPLICATION_JSON)
            .headers(getAuthHeader())
            .content(mapper.writeValueAsString(input))
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
            LocalDate.now()
        );

        assertThat(mvc.post().uri(URI_SAVE_TASK)
            .contentType(MediaType.APPLICATION_JSON)
            .headers(getAuthHeader())
            .content(mapper.writeValueAsString(input))
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
            LocalDate.now()
        );
        TaskDto savedTask = taskService.saveTask(input);

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
            LocalDate.now()
        );
        TaskDto savedTask = taskService.saveTask(input);

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

    private static String authToken;
    private HttpHeaders getAuthHeader() throws Exception {
        if (authToken == null) {
             String strResponse = mvc.post().uri("/todo/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new UserDto("valid-user")))
                 .exchange()
                 .getResponse()
                 .getContentAsString();

             LoginResponse response = mapper.readValue(strResponse, LoginResponse.class);
             authToken = response.token();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer "+authToken);
        return headers;
    }
}
