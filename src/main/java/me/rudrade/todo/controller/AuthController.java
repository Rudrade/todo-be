package me.rudrade.todo.controller;

import me.rudrade.todo.exception.InvalidAccessException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import me.rudrade.todo.dto.response.LoginResponse;
import me.rudrade.todo.dto.UserDto;
import me.rudrade.todo.model.User;
import me.rudrade.todo.service.AuthenticationService;
import me.rudrade.todo.service.JwtService;

import java.util.Optional;

@RestController
@RequestMapping("/todo/auth")
public class AuthController {

	private final JwtService jwtService;
	private final AuthenticationService authenticationService;

	public AuthController(JwtService jwtService, AuthenticationService authenticationService) {
		this.jwtService = jwtService;
		this.authenticationService = authenticationService;
	}
	
	@PostMapping("/login")
	public LoginResponse authenticate(@RequestBody UserDto userDto) {
		Optional<User> authenticatedUser = authenticationService.authenticate(userDto);
        if (authenticatedUser.isEmpty()) {
			throw new InvalidAccessException();
        }
		
		String jwtToken = jwtService.generateToken(authenticatedUser.get().getUsername());
		
		return new LoginResponse(jwtToken, jwtService.getExpirationTime());
	}
	
}
