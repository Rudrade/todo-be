package me.rudrade.todo.config;

import org.springframework.beans.factory.annotation.Value;

import jakarta.annotation.PostConstruct;

public class DatabaseConfig {
	
	@Value("${url}")
	private String url;
	
	@Value("${username}")
	private String username;
	
	@Value("${password}")
	private String password;
	
	@PostConstruct
	private void postConstruct() {
		System.out.println("#######################");
		System.out.println(url);
		System.out.println(username);
		System.out.println(password);
		System.out.println("#######################");
	}

}
