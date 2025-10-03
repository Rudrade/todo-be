package me.rudrade.todo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import me.rudrade.todo.dto.LoginResponse;
import me.rudrade.todo.dto.UserDto;
import me.rudrade.todo.model.User;
import me.rudrade.todo.service.AuthenticationService;
import me.rudrade.todo.service.JwtService;

@RestController
@RequestMapping("/todo/auth")
public class AuthController {

	@Autowired private JwtService jwtService;
	@Autowired private AuthenticationService authenticationService;
	
	@PostMapping("/login")
	public LoginResponse authenticate(@RequestBody UserDto userDto) {
		User authenticatedUser = authenticationService.authenticate(userDto);
		
		String jwtToken = jwtService.generateToken(authenticatedUser.getUsername());
		
		return new LoginResponse(jwtToken, jwtService.getExpirationTime());
	}
	
}
