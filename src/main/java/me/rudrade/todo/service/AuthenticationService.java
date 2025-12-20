package me.rudrade.todo.service;

import org.springframework.stereotype.Service;

import me.rudrade.todo.dto.UserDto;
import me.rudrade.todo.model.User;
import me.rudrade.todo.repository.UserRepository;

import java.util.Optional;

@Service
public class AuthenticationService {

	private final UserRepository userRepository;
	private final JwtService jwtService;

	public AuthenticationService(UserRepository userRepository, JwtService jwtService) {
		this.userRepository = userRepository;
		this.jwtService = jwtService;
	}
	public Optional<User> authenticate(UserDto user) {
		return userRepository.findByUsername(user.username());
	}

	public Optional<User> getUserByAuth(String authToken) {
		String username = jwtService.extractUsername(authToken);

		return userRepository.findByUsername(username);
	}
}
