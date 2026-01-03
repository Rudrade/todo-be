package me.rudrade.todo.service;

import java.util.Date;

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
				.withIssuedAt(new Date(System.currentTimeMillis()))
				.withExpiresAt(new Date(System.currentTimeMillis() + jwtExpiration))
				.sign(getAlgorithm());
	}
	
	private Algorithm getAlgorithm() {
		return Algorithm.HMAC256(secretKey);
	}
	
	public String extractUsername(String token) {
		if (token != null) {
			return JWT.decode(cleanBearer(token)).getClaim(CLAIM_USERNAME).asString();
		}

		return null;
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

	public boolean isTokenValidWithLeeway(@NotBlank String token, @NotBlank String username) {
		var decoded = decodeTokenWithLeeway(token);

		return username.equals(decoded.getClaim(CLAIM_USERNAME).asString());
	}
	
	public boolean isTokenValid(@NotBlank String token, @NotBlank String username) {
		DecodedJWT decodedJwt = decodeJWT(token);
		
		return username.equals(decodedJwt.getClaim(CLAIM_USERNAME).asString()); 
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
