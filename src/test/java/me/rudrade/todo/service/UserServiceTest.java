package me.rudrade.todo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import jakarta.validation.Validation;
import jakarta.validation.executable.ExecutableValidator;
import me.rudrade.todo.dto.UserChangeDto;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserRequestRepository userRequestRepository;

    private UserService userService;
    private UserService service() {
        if (userService == null) {
            userService = new UserService(userRepository, userRequestRepository, new BCryptPasswordEncoder());
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
    void itShouldThrowWhenActiveUserAlreadyExists() {
        var user = new User();
        user.setId(UUID.randomUUID());

        when(userRepository.findActiveByUsernameOrEmail("new-user", "new-user@mail.com"))
            .thenReturn(List.of(user));

        assertThrows(EntityAlreadyExistsException.class, () -> service().createUser(validDto()));

        verify(userRepository, times(1)).findActiveByUsernameOrEmail("new-user", "new-user@mail.com");
        verifyNoInteractions(userRequestRepository);
    }

    @Test
    void itShouldThrowWhenUserRequestAlreadyExists() {
        when(userRepository.findActiveByUsernameOrEmail("new-user", "new-user@mail.com"))
            .thenReturn(List.of());
        when(userRequestRepository.existsByUsernameOrEmail("new-user", "new-user@mail.com"))
            .thenReturn(true);

        assertThrows(EntityAlreadyExistsException.class, () -> service().createUser(validDto()));

        verify(userRepository, times(1)).findActiveByUsernameOrEmail("new-user", "new-user@mail.com");
        verify(userRequestRepository, times(1)).existsByUsernameOrEmail("new-user", "new-user@mail.com");
        verify(userRequestRepository, never()).save(any());
    }

    @Test
    void itShouldCreateUserRequest() {
        when(userRepository.findActiveByUsernameOrEmail("new-user", "new-user@mail.com"))
            .thenReturn(List.of());
        when(userRequestRepository.existsByUsernameOrEmail("new-user", "new-user@mail.com"))
            .thenReturn(false);
        when(userRequestRepository.save(any(UserRequest.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        UserRequest result = service().createUser(validDto());

        assertThat(result)
            .isNotNull()
            .satisfies(saved -> {
                assertThat(saved.getId()).isNull();
                assertThat(saved.getUsername()).isEqualTo("new-user");
                assertThat(saved.getEmail()).isEqualTo("new-user@mail.com");
                assertThat(saved.getRole()).isEqualTo(Role.ROLE_USER);
                assertThat(saved.getDtCreated()).isNotNull();
                assertThat(saved.getDtCreated()).isBeforeOrEqualTo(LocalDateTime.now());

                var matches = new BCryptPasswordEncoder().matches("secret", saved.getPassword());
                assertThat(matches).isTrue();
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

    // ### updateUser ###

    @Test
    void itShouldValidateNotNullParamsOnUpdateUser() throws Exception {
        ExecutableValidator validator = Validation.buildDefaultValidatorFactory()
            .getValidator()
            .forExecutables();
        var method = UserService.class.getMethod("updateUser", UUID.class, UserChangeDto.class, User.class);

        var idViolations = validator.validateParameters(
            service(),
            method,
            new Object[]{null, new UserChangeDto("new-user", null, null, null, null), adminUser()}
        );
        var dataViolations = validator.validateParameters(
            service(),
            method,
            new Object[]{UUID.randomUUID(), null, adminUser()}
        );
        var requesterViolations = validator.validateParameters(
            service(),
            method,
            new Object[]{UUID.randomUUID(), new UserChangeDto("new-user", null, null, null, null), null}
        );

        assertThat(idViolations).hasSize(1);
        assertThat(dataViolations).hasSize(1);
        assertThat(requesterViolations).hasSize(1);
    }

    @Test
    void itShouldThrowWhenNoFieldsProvided() {
        UUID id = UUID.randomUUID();

        assertThrows(
            InvalidDataException.class,
            () -> service().updateUser(id, new UserChangeDto(null, null, null, null, null), adminUser())
        );

        verifyNoInteractions(userRepository, userRequestRepository);
    }

    @Test
    void itShouldThrowWhenUpdatingNonExistingUser() {
        UUID id = UUID.randomUUID();

        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(
            InvalidDataException.class,
            () -> service().updateUser(id, new UserChangeDto("user", null, null, null, null), adminUser())
        );

        verify(userRepository, times(1)).findById(id);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(userRequestRepository);
    }

    @Test
    void itShouldThrowWhenRequesterIsNotAdminNorSelf() {
        UUID id = UUID.randomUUID();
        User stored = new User();
        stored.setId(id);

        when(userRepository.findById(id)).thenReturn(Optional.of(stored));

        User requester = new User();
        requester.setId(UUID.randomUUID());
        requester.setRole(Role.ROLE_USER);

        assertThrows(
            InvalidAccessException.class,
            () -> service().updateUser(id, new UserChangeDto("new-user", null, null, null, null), requester)
        );

        verify(userRepository, times(1)).findById(id);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(userRequestRepository);
    }

    @Test
    void itShouldThrowWhenUsernameOrEmailAlreadyExists() {
        UUID id = UUID.randomUUID();
        User stored = new User();
        stored.setId(id);

        var user = new User();
        user.setId(UUID.randomUUID());

        when(userRepository.findById(id)).thenReturn(Optional.of(stored));
        when(userRepository.findActiveByUsernameOrEmail("new-user", "new@mail.com"))
            .thenReturn(List.of(user));

        assertThrows(
            EntityAlreadyExistsException.class,
            () -> service().updateUser(
                id,
                new UserChangeDto("new-user", null, "new@mail.com", null, null),
                adminUser()
            )
        );
        
        verify(userRepository, times(1)).findById(id);
        verify(userRepository, times(1)).findActiveByUsernameOrEmail("new-user", "new@mail.com");
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(userRequestRepository);
    }
    
    @Test
    void itShouldThrowWhenUpdateUserIsRoleUserUpdatingRole() {
        var user = new User();
        user.setRole(Role.ROLE_USER);

        assertThrows(InvalidAccessException.class,  () -> service().updateUser(UUID.randomUUID(), new UserChangeDto(null, null, null, Role.ROLE_ADMIN, null), user));

        verifyNoInteractions(userRepository, userRequestRepository);
    }

    @Test
    void itShouldThrowWhenUpdateUserIsRoleUserUpdatingStatus() {
        var user = new User();
        user.setRole(Role.ROLE_USER);

        assertThrows(InvalidAccessException.class,  () -> service().updateUser(UUID.randomUUID(), new UserChangeDto(null, null, null, null, Boolean.FALSE), user));

        verifyNoInteractions(userRepository, userRequestRepository);
    }

    @ParameterizedTest
    @MethodSource("itShouldUpdateUserInputs")
    void itShouldUpdateUser(UUID id, UserChangeDto data, User requester, String[] fieldsUpdated) {
        User stored = new User();
        stored.setId(id);
        stored.setUsername("current");
        stored.setEmail("current@mail.com");
        stored.setActive(true);
        stored.setPassword("old-password");
        stored.setRole(Role.ROLE_USER);

        when(userRepository.findById(id)).thenReturn(Optional.of(stored));
        when(userRepository.findActiveByUsernameOrEmail(data.username(), data.email())).thenReturn(List.of());
        when(userRequestRepository.existsByUsernameOrEmail(data.username(), data.email())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = service().updateUser(
            id,
            data,
            requester
        );

        if (data.password() != null) {
            assertThat(result.getPassword()).isNotEqualTo(data.password());
            assertThat(new BCryptPasswordEncoder().matches(data.password(), result.getPassword())).isTrue();
        }

        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFields(fieldsUpdated)
            .isEqualTo(stored);

        var dataUser = new User();
		dataUser.setUsername(data.username());
		dataUser.setPassword(data.password());
		dataUser.setEmail(data.email());
		dataUser.setRole(data.role());
		dataUser.setActive(Boolean.TRUE.equals(data.active()));
        dataUser.setId(id);

        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFields("password")
            .comparingOnlyFields(fieldsUpdated)
            .isEqualTo(dataUser);

        verify(userRepository, times(1)).findById(id);
        verify(userRepository, times(1)).findActiveByUsernameOrEmail(data.username(), data.email());
        verify(userRequestRepository, times(1)).existsByUsernameOrEmail(data.username(), data.email());
        verify(userRepository, times(1)).save(stored);
        verifyNoMoreInteractions(userRepository, userRequestRepository);
    }

    private static Stream<Arguments> itShouldUpdateUserInputs() {
        var id = UUID.randomUUID();
        var user = new User();
        user.setId(id);
        user.setRole(Role.ROLE_USER);

        return Stream.of(
            Arguments.of(id, new UserChangeDto("new-username", null, null, null, null), adminUser(), new String[]{"username"}),
            Arguments.of(id, new UserChangeDto(null, "new-password", null, null, null), adminUser(), new String[]{"password"}),
            Arguments.of(id, new UserChangeDto(null, null, "new-mail@mail.com", null, null), adminUser(), new String[]{"email"}),
            Arguments.of(id, new UserChangeDto(null, null, null, Role.ROLE_ADMIN, null), adminUser(), new String[]{"role"}),
            Arguments.of(id, new UserChangeDto(null, null, null, null, Boolean.FALSE), adminUser(), new String[]{"active"}),
            Arguments.of(id, new UserChangeDto("new-username", "new-password", "new-mail@mail.com", Role.ROLE_ADMIN, Boolean.FALSE), adminUser(), new String[]{"username","password","email","role","active"}),
            Arguments.of(id, new UserChangeDto("new-username", "new-password", "new-mail@mail.com", null, null), user, new String[]{"username","password","email"})
        );
    }


    // ### end updateUser ###

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

