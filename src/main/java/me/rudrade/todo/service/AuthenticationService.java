package me.rudrade.todo.service;

import me.rudrade.todo.dto.response.LoginResponse;
import me.rudrade.todo.exception.InvalidAccessException;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import me.rudrade.todo.dto.UserLoginDto;
import me.rudrade.todo.model.User;
import me.rudrade.todo.repository.UserRepository;

import java.util.Optional;

@Service
@AllArgsConstructor
public class AuthenticationService {

	private final UserRepository userRepository;
	private final JwtService jwtService;
	private final PasswordEncoder passwordEncoder;

	public LoginResponse authenticate(UserLoginDto user) {
		if (user == null ||
			user.username() == null || user.username().isBlank() ||
			user.password() == null || user.password().isBlank())
			throw new InvalidAccessException();

	 	Optional<User>  optUser =  userRepository.findByUsername(user.username());
		 if (optUser.isEmpty())
			 throw new InvalidAccessException();

		 if (!passwordEncoder.matches(user.password(), optUser.get().getPassword()))
			 throw new InvalidAccessException();

		 if (!optUser.get().isActive())
			 throw new InvalidAccessException();

		return new LoginResponse(jwtService.generateToken(optUser.get()));
	}

	public User getUserByAuth(String authToken) {
		if (authToken == null || authToken.isBlank())
			throw new InvalidAccessException();

		String username = jwtService.extractUsername(authToken);
		if (username == null || username.isBlank())
			throw new InvalidAccessException();

		return userRepository.findByUsername(username).orElseThrow(InvalidAccessException::new);
	}

}
