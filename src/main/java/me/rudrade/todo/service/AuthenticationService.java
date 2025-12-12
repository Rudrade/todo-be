package me.rudrade.todo.service;

import org.springframework.stereotype.Service;

import me.rudrade.todo.dto.UserDto;
import me.rudrade.todo.model.User;
import me.rudrade.todo.repository.UserRepository;

import java.util.Optional;

@Service
public class AuthenticationService {

	private final UserRepository userRepository;

	public AuthenticationService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}
	
	public Optional<User> authenticate(UserDto user) {
		return userRepository.findByUsername(user.username());
	}
	
}
