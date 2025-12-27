package me.rudrade.todo.controller;

import org.springframework.web.bind.annotation.*;

import me.rudrade.todo.dto.response.LoginResponse;
import me.rudrade.todo.dto.UserDto;
import me.rudrade.todo.service.AuthenticationService;

@RestController
@RequestMapping("/todo/auth")
public class AuthController {

	private final AuthenticationService authenticationService;

	public AuthController(AuthenticationService authenticationService) {
		this.authenticationService = authenticationService;
	}
	
	@PostMapping("/login")
	public LoginResponse authenticate(@RequestBody UserDto userDto) {
		return authenticationService.authenticate(userDto);
	}

	@GetMapping("/users")
	public String getUsers() {
		return "Not implemented";
	}

	@PostMapping("/users/new")
	public void createUser(@RequestBody UserDto userDto) {
		authenticationService.createUser(userDto);
	}
	
}
