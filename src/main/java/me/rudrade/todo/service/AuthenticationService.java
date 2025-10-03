package me.rudrade.todo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import me.rudrade.todo.dto.UserDto;
import me.rudrade.todo.model.User;
import me.rudrade.todo.repository.UserRepository;

@Service
public class AuthenticationService {

	@Autowired private UserRepository userRepository;
	
	public User authenticate(UserDto user) {
		return userRepository.findByUsername(user.username())
				.orElseThrow();
	}
	
}
