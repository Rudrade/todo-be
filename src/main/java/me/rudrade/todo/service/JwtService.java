package me.rudrade.todo.service;

import java.util.Date;

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

	@Value("${jwt-secret-key}")
	private String secretKey;
	
	@Value("${jwt-expiration-time}")
	private long jwtExpiration;
	
	@Value("${jwt-issuer}")
	private String issuer;
	
	public String generateToken(User user) {
		return JWT.create()
				.withIssuer(issuer)
				.withClaim(CLAIM_USERNAME, user.getUsername())
				.withClaim(CLAIM_ROLE, user.getRole()==null?"":user.getRole().name())
				.withIssuedAt(new Date(System.currentTimeMillis()))
				.withExpiresAt(new Date(System.currentTimeMillis() + jwtExpiration))
				.sign(getAlgorithm());
	}
	
	private Algorithm getAlgorithm() {
		return Algorithm.HMAC256(secretKey);
	}
	
	public String extractUsername(String token) {
		if (token.startsWith("Bearer ")) {
			token = token.substring(7);
		}

		return JWT.decode(token).getClaim(CLAIM_USERNAME).asString();
	}
	
	public boolean isTokenValid(String token, String username) {
		DecodedJWT decodedJwt = JWT.require(getAlgorithm())
				.withIssuer(issuer)
				.build()
				.verify(token);
		
		return username.equals(decodedJwt.getClaim(CLAIM_USERNAME).asString()); 
	}
	
}
