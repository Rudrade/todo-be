package me.rudrade.todo.service;

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

    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private AuthenticationService authenticationService;

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

        when(jwtService.extractUsername("auth-token"))
            .thenReturn("rui");
        when(userRepository.findByUsername("rui"))
            .thenReturn(Optional.of(user));

        User result = getAuthenticationService().getUserByAuth("auth-token");

        assertThat(result).isEqualTo(user);

        verify(jwtService, times(1)).extractUsername("auth-token");
        verify(userRepository, times(1)).findByUsername("rui");
        verifyNoMoreInteractions(jwtService, userRepository);
    }

    @Test
    void itShouldThrowWhenAuthTokenIsNull() {
        AuthenticationService service = getAuthenticationService();

        assertThrows(InvalidAccessException.class, () -> service.getUserByAuth(null));

        verifyNoInteractions(jwtService, userRepository);
    }

    @Test
    void itShouldThrowWhenAuthTokenIsBlank() {
        AuthenticationService service = getAuthenticationService();

        assertThrows(InvalidAccessException.class, () -> service.getUserByAuth("   "));

        verifyNoInteractions(jwtService, userRepository);
    }

    @Test
    void itShouldThrowWhenExtractedUsernameIsNull() {
        AuthenticationService service = getAuthenticationService();
        when(jwtService.extractUsername("auth-token")).thenReturn(null);

        assertThrows(InvalidAccessException.class, () -> service.getUserByAuth("auth-token"));

        verify(jwtService, times(1)).extractUsername("auth-token");
        verifyNoMoreInteractions(jwtService);
        verifyNoInteractions(userRepository);
    }

    @Test
    void itShouldThrowWhenExtractedUsernameIsBlank() {
        AuthenticationService service = getAuthenticationService();
        when(jwtService.extractUsername("auth-token")).thenReturn("   ");

        assertThrows(InvalidAccessException.class, () -> service.getUserByAuth("auth-token"));

        verify(jwtService, times(1)).extractUsername("auth-token");
        verifyNoMoreInteractions(jwtService);
        verifyNoInteractions(userRepository);
    }

    @Test
    void itShouldThrowWhenUserNotFoundByExtractedUsername() {
        AuthenticationService service = getAuthenticationService();
        when(jwtService.extractUsername("auth-token")).thenReturn("rui");
        when(userRepository.findByUsername("rui")).thenReturn(Optional.empty());

        assertThrows(InvalidAccessException.class, () -> service.getUserByAuth("auth-token"));

        verify(jwtService, times(1)).extractUsername("auth-token");
        verify(userRepository, times(1)).findByUsername("rui");
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
            authenticationService = new AuthenticationService(userRepository, jwtService, passwordEncoder);
        }
        return authenticationService;
    }
}
