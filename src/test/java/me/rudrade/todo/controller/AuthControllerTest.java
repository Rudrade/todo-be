package me.rudrade.todo.controller;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import me.rudrade.todo.config.ControllerIntegration;
import me.rudrade.todo.dto.UserLoginDto;
import me.rudrade.todo.dto.response.LoginResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.Calendar;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class AuthControllerTest extends ControllerIntegration {

    private static final String URI_AUTHENTICATE = "/todo/auth/login";
    private static final String URI_REFRESH_TOKEN = "/todo/auth/refresh";

    @Autowired private MockMvcTester mvc;

    @Test
    void itShouldRefreshToken() {
        getTestUser();

        var issuedAt = Calendar.getInstance();
        issuedAt.add(Calendar.SECOND, -30);

        var eat = new Date(issuedAt.getTimeInMillis());
        issuedAt.add(Calendar.MINUTE, -5);
        var iat = new Date(issuedAt.getTimeInMillis());

        var token = JWT.create()
            .withIssuer("test-app")
            .withClaim("username", getTestUser().getUsername())
            .withClaim("role", "test-role")
            .withClaim("refreshes", 3)
            .withIssuedAt(iat)
            .withExpiresAt(eat)
            .sign(Algorithm.HMAC256("test123"));

        var headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer "+token);

        assertThat(mvc.get().uri(URI_REFRESH_TOKEN)
            .headers(headers))
            .hasStatusOk()
            .bodyJson()
            .convertTo(LoginResponse.class)
            .hasNoNullFieldsOrProperties();
    }

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
