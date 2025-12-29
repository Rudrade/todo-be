package me.rudrade.todo.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import me.rudrade.todo.exception.InvalidDataException;
import me.rudrade.todo.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

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
    void itShouldExtractUsernameWithBearerPrefix() {
        User user = user();
        String token = jwtService.generateToken(user);

        assertThat(jwtService.extractUsername("Bearer " + token)).isEqualTo(user.getUsername());
    }

    @Test
    void itShouldExtractUsernameWithoutPrefix() {
        User user = user();
        String token = jwtService.generateToken(user);

        assertThat(jwtService.extractUsername(token)).isEqualTo(user.getUsername());
    }

    @Test
    void itShouldReturnNullWhenExtractingUsernameFromNull() {
        assertThat(jwtService.extractUsername(null)).isNull();
    }

    @Test
    void itShouldValidateToken() {
        User user = user();
        String token = jwtService.generateToken(user);

        assertThat(jwtService.isTokenValid(token, user.getUsername())).isTrue();
    }

    @Test
    void itShouldReturnFalseWhenUsernameDoesNotMatch() {
        User user = user();
        String token = jwtService.generateToken(user);

        assertThat(jwtService.isTokenValid(token, "other-user")).isFalse();
    }

    @Test
    void itShouldReturnFalseWhenTokenOrUsernameIsBlank() {
        assertThat(jwtService.isTokenValid(null, "user")).isFalse();
        assertThat(jwtService.isTokenValid("", "user")).isFalse();
        assertThat(jwtService.isTokenValid("token", null)).isFalse();
        assertThat(jwtService.isTokenValid("token", "   ")).isFalse();
    }

    private User user() {
        User user = new User();
        user.setUsername("john");
        user.setRole(User.Role.ROLE_USER);
        return user;
    }
}

