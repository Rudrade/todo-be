package me.rudrade.todo.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("prod")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class TodoControllerTest {

	@LocalServerPort
	private int port;
	
	@Autowired
	private TestRestTemplate restTemplate;
	
	@Test	
	void testController() {
		String response = restTemplate.getForObject(
				"http://localhost:"+port+"/todo/api/hello",
				String.class);
		assertThat(response).isEqualTo("Hello World !");
	}
}
