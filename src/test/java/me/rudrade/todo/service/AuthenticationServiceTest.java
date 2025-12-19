package me.rudrade.todo.service;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

import me.rudrade.todo.dto.UserDto;
import me.rudrade.todo.model.User;
import me.rudrade.todo.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock private UserRepository userRepository;

    private AuthenticationService authenticationService;

    @Test
    void testAuthenticate() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("rui");

        when(userRepository.findByUsername("rui"))
                .thenReturn(Optional.of(user));

        Optional<User> output = authenticationService().authenticate(new UserDto("rui"));
        assertThat(output)
                .isNotEmpty()
                .isEqualTo(Optional.of(user));

        verify(userRepository, times(1)).findByUsername("rui");
        verifyNoMoreInteractions(userRepository);
    }

    private AuthenticationService authenticationService() {
        if (authenticationService==null) {
            authenticationService = new AuthenticationService(userRepository);
        }
        return authenticationService;
    }
}
