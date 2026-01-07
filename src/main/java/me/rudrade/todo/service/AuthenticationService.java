package me.rudrade.todo.service;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import me.rudrade.todo.dto.response.LoginResponse;
import me.rudrade.todo.exception.InvalidAccessException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import me.rudrade.todo.dto.UserLoginDto;
import me.rudrade.todo.model.User;
import me.rudrade.todo.repository.UserRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

	private final UserRepository userRepository;
	private final JwtService jwtService;
	private final PasswordEncoder passwordEncoder;

	@Value("${todo.app.jwt.allowedRefreshes}")
	private String nrAllowedRefreshes;

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

	public String refreshToken(@NotBlank String authToken) {
		var id = jwtService.getSubjectId(authToken);
		if (id == null)
			throw new InvalidAccessException();

		// Validate if token is valid, ignoring expiration date
		var isTokenValid = jwtService.isTokenValidWithLeeway(authToken, id);
		if (!isTokenValid)
			throw new InvalidAccessException();

		// Validate if token has claim nr_refresh < X, if false, throw ex
		int refreshes = jwtService.getTokenRefreshes(authToken);
		if (Integer.parseInt(nrAllowedRefreshes) <= refreshes)
			throw new InvalidAccessException();

		var user = getUserByAuth(authToken);

		// Update claim with +1 nr_refresh and return generated token
		return jwtService.generateToken(user, refreshes+1);
	}

	public User getUserByAuth(@NotBlank String authToken) {
		var id = jwtService.getSubjectId(authToken);
		if (id == null)
			throw new InvalidAccessException();

		return userRepository.findById(id).orElseThrow(InvalidAccessException::new);
	}

}
