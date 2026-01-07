package me.rudrade.todo.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import me.rudrade.todo.exception.InvalidDataException;
import me.rudrade.todo.model.User;
import me.rudrade.todo.model.types.Role;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private JwtService jwtService;

    private static final String SECRET = "test-secret";
    private static final long EXPIRATION = 3_600_000L;
    private static final String ISSUER = "test-issuer";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", EXPIRATION);
        ReflectionTestUtils.setField(jwtService, "issuer", ISSUER);
    }

    @Test
    void itShouldGenerateTokenWithClaims() {
        User user = user();

        String token = jwtService.generateToken(user);

        DecodedJWT decoded = JWT.require(Algorithm.HMAC256(SECRET))
            .withIssuer(ISSUER)
            .build()
            .verify(token);

        assertThat(decoded.getClaim("username").asString()).isEqualTo(user.getUsername());
        assertThat(decoded.getClaim("role").asString()).isEqualTo(user.getRole().name());
        assertThat(decoded.getExpiresAt()).isAfter(new Date());
    }

    @Test
    void itShouldThrowWhenUserIsNull() {
        assertThrows(InvalidDataException.class, () -> jwtService.generateToken(null));
    }

    @Test
    void itShouldValidateToken() {
        User user = user();
        String token = jwtService.generateToken(user);

        assertThat(jwtService.isTokenValid(token, user.getId())).isTrue();
    }

    @Test
    void itShouldReturnFalseWhenUsernameDoesNotMatch() {
        User user = user();
        String token = jwtService.generateToken(user);

        assertThat(jwtService.isTokenValid(token, UUID.randomUUID())).isFalse();
    }

    private User user() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("john");
        user.setRole(Role.ROLE_USER);
        return user;
    }
}

