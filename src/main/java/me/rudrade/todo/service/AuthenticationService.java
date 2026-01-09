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

@Service
@RequiredArgsConstructor
public class AuthenticationService {

	private final UserRepository userRepository;
	private final JwtService jwtService;
	private final PasswordEncoder passwordEncoder;
	private final S3Service s3Service;

	@Value("${todo.app.jwt.allowedRefreshes}")
	private String nrAllowedRefreshes;

	public LoginResponse authenticate(UserLoginDto user) {
		if (user == null ||
			user.getUsername() == null || user.getUsername().isBlank() ||
			user.getPassword() == null || user.getPassword().isBlank())
			throw new InvalidAccessException();

	 	var oUser  = userRepository.findByUsername(user.getUsername()).orElseThrow(InvalidAccessException::new);

		if (!passwordEncoder.matches(user.getPassword(), oUser.getPassword()))
			 throw new InvalidAccessException();

		 if (!oUser.isActive())
			 throw new InvalidAccessException();

		 String imageUrl = null;
		 if (oUser.getImageVersion() != null) {
			imageUrl = s3Service.getImagePath(oUser.getId(), oUser.getImageVersion());
		 }

		return new LoginResponse(jwtService.generateToken(oUser), imageUrl);
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
