package me.rudrade.todo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.rudrade.todo.dto.UserLoginDto;
import me.rudrade.todo.dto.response.LoginResponse;
import me.rudrade.todo.model.User;
import me.rudrade.todo.model.types.Role;
import me.rudrade.todo.repository.UserRepository;

import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.io.IOException;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureWebTestClient
@Import(ConfigurationUtil.MailSender.class)
public class ControllerIntegration extends SqlIntegrationTest {
    final ObjectMapper mapper = new ObjectMapper();
    {
        mapper.findAndRegisterModules();
    }

    public ObjectMapper mapper() {
        return mapper;
    }

    @Autowired private MockMvcTester mvc;
    @Autowired private UserRepository userRepository;

    private static String authToken;
    public HttpHeaders getAuthHeader() {
        if (authToken == null) {
            getTestUser();
            authToken = getAuthToken(TEST_USERNAME, TEST_PASSWORD);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer "+authToken);
        return headers;
    }

    public HttpHeaders getAuthHeader(String username, String password) {
        var token = getAuthToken(username, password);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return headers;
    }

    private String getAuthToken(String username, String password) {
        try {
            String strResponse = mvc.post().uri("/todo/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new UserLoginDto(username, password)))
                .exchange()
                .getResponse()
                .getContentAsString();

            LoginResponse response = mapper.readValue(strResponse, LoginResponse.class);
            return response.token();
        } catch (IOException e) {
            Assertions.fail(e);
            return null;
        }
    }

    private static String adminAuthToken;
    public HttpHeaders getAdminAuthHeader() {
        final String adminUsername = "admin-user";
        final String adminPassword = "admin-pass";

        if (adminAuthToken == null) {
            userRepository.findByUsername(adminUsername).orElseGet(() -> {
                User admin = new User();
                admin.setUsername(adminUsername);
                admin.setPassword(encoder.encode(adminPassword));
                admin.setEmail("admin@mail.com");
                admin.setRole(Role.ROLE_ADMIN);
                admin.setActive(true);
                return userRepository.save(admin);
            });

            adminAuthToken = getAuthToken(adminUsername, adminPassword);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + adminAuthToken);
        return headers;
    }

}
