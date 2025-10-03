package me.rudrade.todo.service;

import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

@Service
public class JwtService {
	
	private static final String CLAIM_USERNAME = "username";

	@Value("${jwt-secret-key}")
	private String secretKey;
	
	@Value("${jwt-expiration-time}")
	private long jwtExpiration;
	
	@Value("${jwt-issuer}")
	private String issuer;
	
	public long getExpirationTime() {
		return jwtExpiration;
	}
	
	public String generateToken(String username) {
		return buildToken(jwtExpiration, username);
	}
	
	private String buildToken(long expiration, String username) {
		return JWT.create()
				.withIssuer(issuer)
				.withClaim(CLAIM_USERNAME, username)
				.withIssuedAt(new Date(System.currentTimeMillis()))
				.withExpiresAt(new Date(System.currentTimeMillis() + expiration))
				.sign(getAlgorithm());
	}
	
	private Algorithm getAlgorithm() {
		return Algorithm.HMAC256(secretKey);
	}
	
	public String extractUsername(String token) {
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
