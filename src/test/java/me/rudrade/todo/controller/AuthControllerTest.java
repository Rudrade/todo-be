package me.rudrade.todo.controller;

import me.rudrade.todo.config.ControllerIntegration;
import me.rudrade.todo.dto.UserLoginDto;
import me.rudrade.todo.dto.response.LoginResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

class AuthControllerTest extends ControllerIntegration {

    private static final String URI_AUTHENTICATE = "/todo/auth/login";

    @Autowired private MockMvcTester mvc;

    @Test
    void itShouldReturnInvalidAccessWhenUserIsNotFound() throws Exception {
        assertThat(mvc.post().uri(URI_AUTHENTICATE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper().writeValueAsString(new UserLoginDto("random-user", null)))
        ).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void itShouldReturnOkWhenUserIsValid() throws Exception {
        getTestUser();

        assertThat(mvc.post().uri(URI_AUTHENTICATE)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper().writeValueAsString(new UserLoginDto(TEST_USERNAME, TEST_PASSWORD)))
        ).hasStatusOk()
        .bodyJson()
        .convertTo(LoginResponse.class)
        .satisfies(response -> {
            assertThat(response.token()).isNotEmpty();
        });
    }

}
