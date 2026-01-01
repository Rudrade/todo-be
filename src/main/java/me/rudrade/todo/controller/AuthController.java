package me.rudrade.todo.controller;

import org.springframework.web.bind.annotation.*;

import me.rudrade.todo.dto.response.LoginResponse;
import me.rudrade.todo.dto.UserLoginDto;
import me.rudrade.todo.service.AuthenticationService;

@RestController
@RequestMapping("/todo/auth")
public class AuthController {

	private final AuthenticationService authenticationService;

	public AuthController(AuthenticationService authenticationService) {
		this.authenticationService = authenticationService;
	}
	
	@PostMapping("/login")
	public LoginResponse authenticate(@RequestBody UserLoginDto userDto) {
		return authenticationService.authenticate(userDto);
	}

}
