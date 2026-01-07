package me.rudrade.todo.service;

import java.util.Date;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import me.rudrade.todo.exception.InvalidDataException;
import me.rudrade.todo.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

@Service
public class JwtService {

	private static final String CLAIM_USERNAME = "username";
	private static final String CLAIM_ROLE = "role";
	private static final String CLAIM_REFRESHES = "refreshes";

	@Value("${jwt-secret-key}")
	private String secretKey;
	
	@Value("${jwt-expiration-time}")
	private long jwtExpiration;
	
	@Value("${jwt-issuer}")
	private String issuer;

	public String generateToken(User user) {
		return generateToken(user, 0);
	}
	
	public String generateToken(User user, int refresh) {
		if (user == null)
			throw new InvalidDataException("User must exist to generate a token.");

		return JWT.create()
				.withIssuer(issuer)
				.withClaim(CLAIM_USERNAME, user.getUsername())
				.withClaim(CLAIM_ROLE, user.getRole()==null?"":user.getRole().name())
				.withClaim(CLAIM_REFRESHES, refresh)
				.withSubject(user.getId().toString())
				.withIssuedAt(new Date(System.currentTimeMillis()))
				.withExpiresAt(new Date(System.currentTimeMillis() + jwtExpiration))
				.sign(getAlgorithm());
	}
	
	private Algorithm getAlgorithm() {
		return Algorithm.HMAC256(secretKey);
	}
	
	public UUID getSubjectId(@NotBlank String authToken) {
		try {
			var strId = JWT.decode(cleanBearer(authToken)).getSubject();
			return UUID.fromString(strId);
		} catch (IllegalArgumentException | NullPointerException e) {
			return null;
		}
	}

	public String cleanBearer(@NotBlank String token) {
		if (token.startsWith("Bearer "))
			return token.substring(7);

		return token;
	}

	private DecodedJWT decodeTokenWithLeeway(@NotBlank String token) {
		return JWT.require(getAlgorithm())
			.withIssuer(issuer)
			.acceptLeeway(60)
			.build()
			.verify(token);
	}

	public boolean isTokenValidWithLeeway(@NotBlank String token, @NotBlank UUID id) {
		var decoded = decodeTokenWithLeeway(token);

		return id.toString().equals(decoded.getSubject());
	}
	
	public boolean isTokenValid(@NotBlank String token, @NotBlank UUID id) {
		var decodedJwt = decodeJWT(token);
		
		return id.toString().equals(decodedJwt.getSubject()); 
	}

	private DecodedJWT decodeJWT(String token) {
		return JWT.require(getAlgorithm())
			.withIssuer(issuer)
			.build()
			.verify(token);
	}

	public int getTokenRefreshes(String token) {
		var decoded = decodeTokenWithLeeway(token);
		return decoded.getClaim(CLAIM_REFRESHES).asInt();
	}
	
}
