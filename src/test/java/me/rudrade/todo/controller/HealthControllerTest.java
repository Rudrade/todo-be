package me.rudrade.todo.controller;

import me.rudrade.todo.config.ConfigurationUtil;
import me.rudrade.todo.config.SqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureWebTestClient
@Import(ConfigurationUtil.MailSender.class)
class HealthControllerTest extends SqlIntegrationTest {

    @Autowired private MockMvcTester mvc;

    @Test
    void itShouldReturnOk() {
        assertThat(mvc.get().uri("/health"))
                .hasStatusOk()
                .hasBodyTextEqualTo("OK");
    }
}
