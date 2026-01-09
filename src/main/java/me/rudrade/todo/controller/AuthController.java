package me.rudrade.todo.controller;

import lombok.RequiredArgsConstructor;
import me.rudrade.todo.service.JwtService;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import me.rudrade.todo.dto.response.LoginResponse;
import me.rudrade.todo.dto.UserLoginDto;
import me.rudrade.todo.service.AuthenticationService;

@RestController
@RequestMapping("/todo/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthenticationService authenticationService;
	private final JwtService jwtService;

	@PostMapping("/login")
	public LoginResponse authenticate(@RequestBody UserLoginDto userDto) {
		return authenticationService.authenticate(userDto);
	}

	@GetMapping("/refresh")
	public LoginResponse refreshToken(@RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
		return new LoginResponse(authenticationService.refreshToken(jwtService.cleanBearer(token)), null);
	}

}
