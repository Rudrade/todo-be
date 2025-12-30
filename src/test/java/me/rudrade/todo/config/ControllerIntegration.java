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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.io.IOException;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureWebTestClient
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

            try {
                String strResponse = mvc.post().uri("/todo/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(new UserLoginDto(TEST_USERNAME, TEST_PASSWORD)))
                    .exchange()
                    .getResponse()
                    .getContentAsString();

                LoginResponse response = mapper.readValue(strResponse, LoginResponse.class);
                authToken = response.token();
            } catch (IOException e) {
                Assertions.fail(e);
            }
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer "+authToken);
        return headers;
    }

    private static String adminAuthToken;
    public HttpHeaders getAdminAuthHeader() throws Exception {
        final String adminUsername = "admin-user";
        final String adminPassword = "admin-pass";

        if (adminAuthToken == null) {
            userRepository.findByUsername(adminUsername).orElseGet(() -> {
                PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
                User admin = new User();
                admin.setUsername(adminUsername);
                admin.setPassword(encoder.encode(adminPassword));
                admin.setEmail("admin@mail.com");
                admin.setRole(Role.ROLE_ADMIN);
                admin.setActive(true);
                return userRepository.save(admin);
            });

            String response = mvc.post().uri("/todo/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper().writeValueAsString(new UserLoginDto(adminUsername, adminPassword)))
                .exchange()
                .getResponse()
                .getContentAsString();
            adminAuthToken = mapper().readValue(response, LoginResponse.class).token();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + adminAuthToken);
        return headers;
    }

}
