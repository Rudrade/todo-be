package me.rudrade.todo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

import me.rudrade.todo.dto.UserRequestDto;
import me.rudrade.todo.dto.types.UserSearchType;
import me.rudrade.todo.exception.EntityAlreadyExistsException;
import me.rudrade.todo.exception.InvalidAccessException;
import me.rudrade.todo.exception.InvalidDataException;
import me.rudrade.todo.model.User;
import me.rudrade.todo.model.UserRequest;
import me.rudrade.todo.model.types.Role;
import me.rudrade.todo.repository.UserRepository;
import me.rudrade.todo.repository.UserRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserRequestRepository userRequestRepository;

    private UserService userService;
    private UserService service() {
        if (userService == null) {
            userService = new UserService(userRepository, userRequestRepository);
        }
        return userService;
    }

    private static User adminUser() {
        User user = new User();
        user.setRole(Role.ROLE_ADMIN);
        return user;
    }

    private static UserRequestDto validDto() {
        return new UserRequestDto("new-user", "secret", "new-user@mail.com", Role.ROLE_USER);
    }

    @Test
    void itShouldThrowWhenCreatedByIsNotAdmin() {
        User createdBy = new User();
        createdBy.setRole(Role.ROLE_USER);

        assertThrows(InvalidAccessException.class, () -> service().createUser(validDto(), createdBy));

        verifyNoInteractions(userRepository, userRequestRepository);
    }

    @Test
    void itShouldThrowWhenActiveUserAlreadyExists() {
        when(userRepository.findActiveByUsernameOrEmail("new-user", "new-user@mail.com"))
            .thenReturn(Optional.of(new User()));

        assertThrows(EntityAlreadyExistsException.class, () -> service().createUser(validDto(), adminUser()));

        verify(userRepository, times(1)).findActiveByUsernameOrEmail("new-user", "new-user@mail.com");
        verifyNoInteractions(userRequestRepository);
    }

    @Test
    void itShouldThrowWhenUserRequestAlreadyExists() {
        when(userRepository.findActiveByUsernameOrEmail("new-user", "new-user@mail.com"))
            .thenReturn(Optional.empty());
        when(userRequestRepository.existsByUsernameOrEmail("new-user", "new-user@mail.com"))
            .thenReturn(true);

        assertThrows(EntityAlreadyExistsException.class, () -> service().createUser(validDto(), adminUser()));

        verify(userRepository, times(1)).findActiveByUsernameOrEmail("new-user", "new-user@mail.com");
        verify(userRequestRepository, times(1)).existsByUsernameOrEmail("new-user", "new-user@mail.com");
        verify(userRequestRepository, never()).save(any());
    }

    @Test
    void itShouldCreateUserRequest() {
        when(userRepository.findActiveByUsernameOrEmail("new-user", "new-user@mail.com"))
            .thenReturn(Optional.empty());
        when(userRequestRepository.existsByUsernameOrEmail("new-user", "new-user@mail.com"))
            .thenReturn(false);
        when(userRequestRepository.save(any(UserRequest.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        UserRequest result = service().createUser(validDto(), adminUser());

        assertThat(result)
            .isNotNull()
            .satisfies(saved -> {
                assertThat(saved.getId()).isNull();
                assertThat(saved.getUsername()).isEqualTo("new-user");
                assertThat(saved.getEmail()).isEqualTo("new-user@mail.com");
                assertThat(saved.getPassword()).isEqualTo("secret");
                assertThat(saved.getRole()).isEqualTo(Role.ROLE_USER);
                assertThat(saved.getDtCreated()).isNotNull();
                assertThat(saved.getDtCreated()).isBeforeOrEqualTo(LocalDateTime.now());
            });

        verify(userRepository, times(1)).findActiveByUsernameOrEmail("new-user", "new-user@mail.com");
        verify(userRequestRepository, times(1)).existsByUsernameOrEmail("new-user", "new-user@mail.com");
        verify(userRequestRepository, times(1)).save(argThat(req ->
            req.getDtCreated() != null &&
            "new-user".equals(req.getUsername()) &&
            "new-user@mail.com".equals(req.getEmail()) &&
            Role.ROLE_USER.equals(req.getRole())
        ));
        verifyNoMoreInteractions(userRepository, userRequestRepository);
    }

    @Test
    void itShouldThrowWhenGetByIdCalledByNonAdmin() {
        User requester = new User();
        requester.setRole(Role.ROLE_USER);

        assertThrows(InvalidAccessException.class, () -> service().getById(UUID.randomUUID(), requester));

        verifyNoInteractions(userRepository, userRequestRepository);
    }

    @Test
    void itShouldThrowWhenUserNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(InvalidDataException.class, () -> service().getById(id, adminUser()));

        verify(userRepository, times(1)).findById(id);
        verifyNoInteractions(userRequestRepository);
    }

    @Test
    void itShouldReturnUserWhenFound() {
        UUID id = UUID.randomUUID();
        User expected = new User();
        expected.setId(id);
        when(userRepository.findById(id)).thenReturn(Optional.of(expected));

        User result = service().getById(id, adminUser());

        assertThat(result).isEqualTo(expected);
        verify(userRepository, times(1)).findById(id);
        verifyNoInteractions(userRequestRepository);
    }

    @Test
    void itShouldThrowWhenDeactivateCalledByNonAdmin() {
        User requester = new User();
        requester.setRole(Role.ROLE_USER);

        assertThrows(InvalidAccessException.class, () -> service().deactivateById(UUID.randomUUID(), requester));

        verifyNoInteractions(userRepository, userRequestRepository);
    }

    @Test
    void itShouldThrowWhenDeactivateUserNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(InvalidDataException.class, () -> service().deactivateById(id, adminUser()));

        verify(userRepository, times(1)).findById(id);
        verify(userRepository, never()).save(any());
        verifyNoInteractions(userRequestRepository);
    }

    @Test
    void itShouldDeactivateUser() {
        UUID id = UUID.randomUUID();
        User user = new User();
        user.setId(id);
        user.setActive(true);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        service().deactivateById(id, adminUser());

        assertThat(user.isActive()).isFalse();
        verify(userRepository, times(1)).findById(id);
        verify(userRepository, times(1)).save(user);
        verifyNoInteractions(userRequestRepository);
    }

    @Test
    void itShouldThrowWhenGetAllUsersCalledByNonAdmin() {
        User requester = new User();
        requester.setRole(Role.ROLE_USER);

        assertThrows(InvalidAccessException.class, () -> service().getAllUsers(null, null, null, requester));

        verifyNoInteractions(userRepository, userRequestRepository);
    }

    @Test
    void itShouldThrowWhenSearchTypeWithoutTerm() {
        assertThrows(InvalidDataException.class, () -> service().getAllUsers(null, UserSearchType.USERNAME, " ", adminUser()));
        verifyNoInteractions(userRepository, userRequestRepository);
    }

    @Test
    void itShouldGetAllUsersNoFilter() {
        User user1 = new User();
        user1.setUsername("a");
        user1.setEmail("a@mail.com");
        user1.setRole(Role.ROLE_USER);
        user1.setActive(true);
        when(userRepository.findAll()).thenReturn(List.of(user1));

        var result = service().getAllUsers(null, null, null, adminUser());

        assertThat(result).hasSize(1);
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void itShouldFilterByActive() {
        User user1 = new User(); user1.setActive(true);
        when(userRepository.findByActive(true)).thenReturn(List.of(user1));

        var result = service().getAllUsers(true, null, null, adminUser());

        assertThat(result).hasSize(1);
        verify(userRepository, times(1)).findByActive(true);
    }

    @Test
    void itShouldFilterByUsername() {
        User user1 = new User(); user1.setUsername("john"); user1.setActive(true);
        when(userRepository.findByUsernameContainingIgnoreCase("jo")).thenReturn(List.of(user1));

        var result = service().getAllUsers(null, UserSearchType.USERNAME, "jo", adminUser());

        assertThat(result).hasSize(1);
        verify(userRepository, times(1)).findByUsernameContainingIgnoreCase("jo");
    }

    @Test
    void itShouldFilterByEmailAndActive() {
        User user1 = new User(); user1.setEmail("mail@test.com"); user1.setActive(false);
        when(userRepository.findByActiveAndEmailContainingIgnoreCase(false, "mail")).thenReturn(List.of(user1));

        var result = service().getAllUsers(false, UserSearchType.EMAIL, "mail", adminUser());

        assertThat(result).hasSize(1);
        verify(userRepository, times(1)).findByActiveAndEmailContainingIgnoreCase(false, "mail");
    }
}

