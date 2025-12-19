package me.rudrade.todo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.rudrade.todo.config.SqlIntegrationTest;
import me.rudrade.todo.dto.UserDto;
import me.rudrade.todo.dto.response.LoginResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureWebTestClient
@Sql("/sql-scripts/INIT_USERS.sql")
class AuthControllerTest extends SqlIntegrationTest {

    private static final String URI_AUTHENTICATE = "/todo/auth/login";

    @Autowired
    MockMvcTester mvc;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void itShouldReturnInvalidAccessWhenUserIsNotFound() throws Exception {
        assertThat(mvc.post().uri(URI_AUTHENTICATE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new UserDto("random-user")))
        ).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void itShouldReturnOkWhenUserIsValid() throws Exception {
        assertThat(mvc.post().uri(URI_AUTHENTICATE)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(new UserDto("valid-user")))
        ).hasStatusOk()
        .bodyJson()
        .convertTo(LoginResponse.class)
        .satisfies(response -> {
            assertThat(response.token()).isNotEmpty();
            assertThat(response.expiresIn()).isPositive();
        });
    }
}
