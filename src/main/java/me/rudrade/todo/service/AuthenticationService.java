package me.rudrade.todo.service;

import me.rudrade.todo.dto.response.LoginResponse;
import me.rudrade.todo.exception.InvalidAccessException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import me.rudrade.todo.dto.UserDto;
import me.rudrade.todo.model.User;
import me.rudrade.todo.repository.UserRepository;

import java.util.Optional;

@Service
public class AuthenticationService {

	private final UserRepository userRepository;
	private final JwtService jwtService;
	private final PasswordEncoder passwordEncoder;

	public AuthenticationService(UserRepository userRepository, JwtService jwtService) {
		this.userRepository = userRepository;
		this.jwtService = jwtService;

		this.passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}
	public LoginResponse authenticate(UserDto user) {
		if (user == null || user.username() == null || user.password() == null || user.password().isEmpty())
			throw new InvalidAccessException();

	 	Optional<User>  optUser =  userRepository.findByUsername(user.username());
		 if (optUser.isEmpty())
			 throw new InvalidAccessException();

		 if (!passwordEncoder.matches(user.password(), optUser.get().getPassword()))
			 throw new InvalidAccessException();

		return new LoginResponse(jwtService.generateToken(optUser.get()));
	}

	public Optional<User> getUserByAuth(String authToken) {
		String username = jwtService.extractUsername(authToken);

		return userRepository.findByUsername(username);
	}

	public void createUser(UserDto userDto) {
		User user = new User();
		user.setUsername(userDto.username());
		user.setRole(User.Role.ROLE_USER);
		user.setPassword(passwordEncoder.encode(userDto.password()));

		userRepository.save(user);
	}

}
