package me.rudrade.todo.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import me.rudrade.todo.dto.UserLoginDto;
import me.rudrade.todo.dto.response.LoginResponse;
import me.rudrade.todo.exception.InvalidAccessException;
import me.rudrade.todo.model.User;
import me.rudrade.todo.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Calendar;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private JwtService jwtService;
    @Mock private S3Service s3Service;

    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private AuthenticationService authenticationService;

    private static final String JWT_SECRET_KEY = "test123";
    private static final String JWT_ISSUER = "test-app";

    private Algorithm getAlgorithm() {
        return Algorithm.HMAC256(JWT_SECRET_KEY);
    }

    // ### refreshToken ###

    @Test
    void itShouldRefreshToken() {
        var issuedAt = Calendar.getInstance();
        issuedAt.add(Calendar.MINUTE, -10);

        var id = UUID.randomUUID();

        var token = JWT.create()
            .withIssuer(JWT_ISSUER)
            .withClaim("username", "test-user")
            .withClaim("role", "test-role")
            .withClaim("refreshes", 3)
            .withSubject(id.toString())
            .withIssuedAt(issuedAt.getTime())
            .withExpiresAt(issuedAt.getTime())
            .sign(getAlgorithm());

        var user = new User();
        user.setId(id);

        when(jwtService.getSubjectId(token)).thenReturn(id);
        when(jwtService.isTokenValidWithLeeway(token, id)).thenReturn(true);
        when(jwtService.getTokenRefreshes(token)).thenReturn(4);
        when(jwtService.generateToken(any(User.class), anyInt())).thenCallRealMethod();
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        var result = getAuthenticationService().refreshToken(token);
        assertThat(result).isNotBlank();

        var decodedRefreshes = JWT.require(getAlgorithm())
            .withIssuer(JWT_ISSUER)
            .build()
            .verify(result)
            .getClaim("refreshes").asInt();
        assertThat(decodedRefreshes).isEqualTo(5);

        verify(jwtService, times(1)).isTokenValidWithLeeway(token, id);
        verify(jwtService, times(1)).getTokenRefreshes(token);
        verify(jwtService, times(2)).getSubjectId(token);
        verify(jwtService, times(1)).generateToken(any(User.class), eq(5));
        verifyNoMoreInteractions(jwtService);

        verify(userRepository, times(1)).findById(id);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void itShouldThrowWhenTokenIsInvalid() {
        var id = UUID.randomUUID();
        when(jwtService.getSubjectId("test-token")).thenReturn(id);
        when(jwtService.isTokenValidWithLeeway("test-token", id)).thenReturn(false);

        assertThrows(InvalidAccessException.class, () -> {
           getAuthenticationService().refreshToken("test-token");
        });

        verify(jwtService, times(1)).getSubjectId("test-token");
        verify(jwtService, times(1)).isTokenValidWithLeeway("test-token", id);
        verifyNoMoreInteractions(jwtService);
        verifyNoInteractions(userRepository);
    }

    @Test
    void itShouldThrowWhenNrRefreshesIsInvalid() {
        var issuedAt = Calendar.getInstance();
        issuedAt.add(Calendar.MINUTE, -10);

        var id = UUID.randomUUID();

        var token = JWT.create()
            .withIssuer(JWT_ISSUER)
            .withClaim("username", "test-user")
            .withClaim("role", "test-role")
            .withClaim("refreshes", 5)
            .withSubject(id.toString())
            .withIssuedAt(issuedAt.getTime())
            .withExpiresAt(issuedAt.getTime())
            .sign(getAlgorithm());

        when(jwtService.getSubjectId(token)).thenReturn(id);
        when(jwtService.isTokenValidWithLeeway(token, id)).thenReturn(true);
        when(jwtService.getTokenRefreshes(token)).thenReturn(5);

        assertThrows(InvalidAccessException.class, () -> getAuthenticationService().refreshToken(token));

        verify(jwtService, times(1)).getSubjectId(token);
        verify(jwtService, times(1)).isTokenValidWithLeeway(token, id);
        verify(jwtService, times(1)).getTokenRefreshes(token);
        verifyNoMoreInteractions(jwtService, userRepository);
    }

    @Test
    void itShouldThrowWhenUserIsNotFoundWhenRefreshingToken() {
        var issuedAt = Calendar.getInstance();
        issuedAt.add(Calendar.MINUTE, -10);

        var id = UUID.randomUUID();

        var token = JWT.create()
            .withIssuer(JWT_ISSUER)
            .withClaim("username", "test-user")
            .withClaim("role", "test-role")
            .withClaim("refreshes", 3)
            .withSubject(id.toString())
            .withIssuedAt(issuedAt.getTime())
            .withExpiresAt(issuedAt.getTime())
            .sign(getAlgorithm());

        when(jwtService.getSubjectId(token)).thenReturn(id);
        when(jwtService.isTokenValidWithLeeway(token, id)).thenReturn(true);
        when(jwtService.getTokenRefreshes(token)).thenReturn(3);
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(InvalidAccessException.class, () -> getAuthenticationService().refreshToken(token));

        verify(jwtService, times(2)).getSubjectId(token);
        verify(jwtService, times(1)).isTokenValidWithLeeway(token, id);
        verify(jwtService, times(1)).getTokenRefreshes(token);
        verify(userRepository, times(1)).findById(id);
        verifyNoMoreInteractions(jwtService, userRepository);
    }

    // ### end refreshToken ###

    @Test
    void itShouldAuthenticateSuccessfully() {
        String rawPassword = "password";

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("rui");
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setActive(true);

        when(userRepository.findByUsername("rui"))
            .thenReturn(Optional.of(user));
        when(jwtService.generateToken(user))
            .thenReturn("token");

        LoginResponse response = getAuthenticationService().authenticate(new UserLoginDto("rui", rawPassword));

        assertThat(response.token())
            .isEqualTo("token");

        verify(userRepository, times(1)).findByUsername("rui");
        verify(jwtService, times(1)).generateToken(user);
        verifyNoMoreInteractions(userRepository, jwtService);
    }

    @ParameterizedTest
    @MethodSource("invalidAuthenticationInputs")
    void itShouldThrowWhenAuthenticatingWithInvalidInput(UserLoginDto invalidInput) {
        AuthenticationService service = getAuthenticationService();

        assertThrows(InvalidAccessException.class, () -> service.authenticate(invalidInput));

        verifyNoInteractions(userRepository, jwtService);
    }

    @Test
    void itShouldThrowWhenUserIsNotFound() {
        AuthenticationService service = getAuthenticationService();
        when(userRepository.findByUsername("rui"))
            .thenReturn(Optional.empty());
        UserLoginDto userDto = new UserLoginDto("rui", "password");

        assertThrows(InvalidAccessException.class, () -> service.authenticate(userDto));

        verify(userRepository, times(1)).findByUsername("rui");
        verifyNoInteractions(jwtService);
    }

    @Test
    void itShouldThrowWhenPasswordDoesNotMatch() {
        AuthenticationService service = getAuthenticationService();

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("rui");
        user.setPassword(passwordEncoder.encode("different-password"));
        user.setActive(true);

        when(userRepository.findByUsername("rui"))
            .thenReturn(Optional.of(user));
        UserLoginDto userDto = new UserLoginDto("rui", "password");

        assertThrows(InvalidAccessException.class, () -> service.authenticate(userDto));

        verify(userRepository, times(1)).findByUsername("rui");
        verifyNoInteractions(jwtService);
    }

    @Test
    void itShouldGetUserByAuthToken() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("rui");
        user.setActive(true);

        when(jwtService.getSubjectId("auth-token"))
            .thenReturn(user.getId());
        when(userRepository.findById(user.getId()))
            .thenReturn(Optional.of(user));

        User result = getAuthenticationService().getUserByAuth("auth-token");

        assertThat(result).isEqualTo(user);

        verify(jwtService, times(1)).getSubjectId("auth-token");
        verify(userRepository, times(1)).findById(user.getId());
        verifyNoMoreInteractions(jwtService, userRepository);
    }

    @Test
    void itShouldThrowWhenUserNotFoundByExtractedId() {
        var id = UUID.randomUUID();

        AuthenticationService service = getAuthenticationService();
        when(jwtService.getSubjectId("auth-token")).thenReturn(id);
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(InvalidAccessException.class, () -> service.getUserByAuth("auth-token"));

        verify(jwtService, times(1)).getSubjectId("auth-token");
        verify(userRepository, times(1)).findById(id);
        verifyNoMoreInteractions(jwtService, userRepository);
    }

    @Test
    void itShouldThrowWhenUserInactiveOnAuthenticate() {
        AuthenticationService service = getAuthenticationService();

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("rui");
        user.setPassword(passwordEncoder.encode("password"));
        user.setActive(false);

        when(userRepository.findByUsername("rui"))
            .thenReturn(Optional.of(user));
        UserLoginDto userDto = new UserLoginDto("rui", "password");

        assertThrows(InvalidAccessException.class, () -> service.authenticate(userDto));

        verify(userRepository, times(1)).findByUsername("rui");
        verifyNoInteractions(jwtService);
    }

    private static Stream<UserLoginDto> invalidAuthenticationInputs() {
        return Stream.of(
            null,
            new UserLoginDto(null, "password"),
            new UserLoginDto("   ", "password"),
            new UserLoginDto("rui", null),
            new UserLoginDto("rui", ""),
            new UserLoginDto("rui", "   ")
        );
    }

    private AuthenticationService getAuthenticationService() {
        if (authenticationService == null) {
            authenticationService = new AuthenticationService(userRepository, jwtService, passwordEncoder, s3Service);
            ReflectionTestUtils.setField(authenticationService, "nrAllowedRefreshes", "5");
            ReflectionTestUtils.setField(jwtService, "secretKey", "test123");
            ReflectionTestUtils.setField(jwtService, "jwtExpiration", 6000L);
            ReflectionTestUtils.setField(jwtService, "issuer", "test-app");
        }
        return authenticationService;
    }
}
