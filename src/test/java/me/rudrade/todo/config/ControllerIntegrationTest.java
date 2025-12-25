package me.rudrade.todo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.rudrade.todo.dto.UserDto;
import me.rudrade.todo.dto.response.LoginResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureWebTestClient
@Sql("/sql-scripts/INIT_USERS.sql")
public class ControllerIntegrationTest extends SqlIntegrationTest {

    final ObjectMapper mapper = new ObjectMapper();
    {
        mapper.findAndRegisterModules();
    }

    public ObjectMapper mapper() {
        return mapper;
    }

    @Autowired private MockMvcTester mvc;

    private static String authToken;
    public HttpHeaders getAuthHeader() throws Exception {
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
