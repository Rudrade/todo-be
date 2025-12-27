package me.rudrade.todo.service;

import me.rudrade.todo.dto.UserDto;
import me.rudrade.todo.dto.response.LoginResponse;
import me.rudrade.todo.exception.InvalidAccessException;
import me.rudrade.todo.model.User;
import me.rudrade.todo.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private JwtService jwtService;

    private AuthenticationService authenticationService;

    @Test
    void itShouldAuthenticateSuccessfully() {
        PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        String rawPassword = "password";

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("rui");
        user.setPassword(passwordEncoder.encode(rawPassword));

        when(userRepository.findByUsername("rui"))
            .thenReturn(Optional.of(user));
        when(jwtService.generateToken(user))
            .thenReturn("token");

        LoginResponse response = getAuthenticationService().authenticate(new UserDto("rui", rawPassword));

        assertThat(response.token())
            .isEqualTo("token");

        verify(userRepository, times(1)).findByUsername("rui");
        verify(jwtService, times(1)).generateToken(user);
        verifyNoMoreInteractions(userRepository, jwtService);
    }

    @Test
    void itShouldThrowWhenUserDtoIsNull() {
        assertThatThrownBy(() -> getAuthenticationService().authenticate(null))
            .isInstanceOf(InvalidAccessException.class);

        verifyNoInteractions(userRepository, jwtService);
    }

    @Test
    void itShouldThrowWhenUsernameIsNull() {
        assertThatThrownBy(() -> getAuthenticationService().authenticate(new UserDto(null, "password")))
            .isInstanceOf(InvalidAccessException.class);

        verifyNoInteractions(userRepository, jwtService);
    }

    @Test
    void itShouldThrowWhenPasswordIsNull() {
        assertThatThrownBy(() -> getAuthenticationService().authenticate(new UserDto("rui", null)))
            .isInstanceOf(InvalidAccessException.class);

        verifyNoInteractions(userRepository, jwtService);
    }

    @Test
    void itShouldThrowWhenPasswordIsEmpty() {
        assertThatThrownBy(() -> getAuthenticationService().authenticate(new UserDto("rui", "")))
            .isInstanceOf(InvalidAccessException.class);

        verifyNoInteractions(userRepository, jwtService);
    }

    @Test
    void itShouldThrowWhenUserIsNotFound() {
        when(userRepository.findByUsername("rui"))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> getAuthenticationService().authenticate(new UserDto("rui", "password")))
            .isInstanceOf(InvalidAccessException.class);

        verify(userRepository, times(1)).findByUsername("rui");
        verifyNoInteractions(jwtService);
    }

    @Test
    void itShouldThrowWhenPasswordDoesNotMatch() {
        PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("rui");
        user.setPassword(passwordEncoder.encode("different-password"));

        when(userRepository.findByUsername("rui"))
            .thenReturn(Optional.of(user));

        assertThatThrownBy(() -> getAuthenticationService().authenticate(new UserDto("rui", "password")))
            .isInstanceOf(InvalidAccessException.class);

        verify(userRepository, times(1)).findByUsername("rui");
        verifyNoInteractions(jwtService);
    }

    @Test
    void itShouldGetUserByAuthToken() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("rui");

        when(jwtService.extractUsername("auth-token"))
            .thenReturn("rui");
        when(userRepository.findByUsername("rui"))
            .thenReturn(Optional.of(user));

        Optional<User> result = getAuthenticationService().getUserByAuth("auth-token");

        assertThat(result)
            .hasValue(user);

        verify(jwtService, times(1)).extractUsername("auth-token");
        verify(userRepository, times(1)).findByUsername("rui");
        verifyNoMoreInteractions(jwtService, userRepository);
    }

    @Test
    void itShouldCreateUserWithEncodedPasswordAndRole() {
        PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

        getAuthenticationService().createUser(new UserDto("rui", "password"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());
        verifyNoInteractions(jwtService);
        verifyNoMoreInteractions(userRepository);

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getUsername())
            .isEqualTo("rui");
        assertThat(savedUser.getRole())
            .isEqualTo(User.Role.ROLE_USER);
        assertThat(savedUser.getPassword())
            .isNotEqualTo("password");
        assertThat(passwordEncoder.matches("password", savedUser.getPassword()))
            .isTrue();
    }

    private AuthenticationService getAuthenticationService() {
        if (authenticationService == null) {
            authenticationService = new AuthenticationService(userRepository, jwtService);
        }
        return authenticationService;
    }
}
