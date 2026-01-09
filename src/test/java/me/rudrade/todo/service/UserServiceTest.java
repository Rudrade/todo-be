package me.rudrade.todo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.validation.Validation;
import jakarta.validation.executable.ExecutableValidator;
import me.rudrade.todo.dto.Mapper;
import me.rudrade.todo.dto.UserChangeDto;
import me.rudrade.todo.dto.UserDto;
import me.rudrade.todo.dto.UserRequestDto;
import me.rudrade.todo.dto.types.UserSearchType;
import me.rudrade.todo.exception.EntityAlreadyExistsException;
import me.rudrade.todo.exception.InvalidAccessException;
import me.rudrade.todo.exception.InvalidDataException;
import me.rudrade.todo.model.User;
import me.rudrade.todo.model.UserRequest;
import me.rudrade.todo.model.types.Role;
import me.rudrade.todo.repository.PasswordRequestRepository;
import me.rudrade.todo.repository.UserRepository;
import me.rudrade.todo.repository.UserRequestRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock private UserRepository userRepository;
    @Mock private UserRequestRepository userRequestRepository;
    @Mock private MailService mailService;
    @Mock private PasswordRequestRepository passwordRequestRepository;
    @Mock private S3Service s3Service;

    private UserService userService;

    @BeforeEach
    void setup() {
        userService = new UserService(userRepository, userRequestRepository, new BCryptPasswordEncoder(), mailService, passwordRequestRepository, s3Service);
    }

    private static User adminUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
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

        var request = validDto();
        assertThrows(EntityAlreadyExistsException.class, () -> userService.createUser(request));

        verify(userRepository, times(1)).findActiveByUsernameOrEmail("new-user", "new-user@mail.com");
        verifyNoInteractions(userRequestRepository);
    }

    @Test
    void itShouldThrowWhenUserRequestAlreadyExists() {
        when(userRepository.findActiveByUsernameOrEmail("new-user", "new-user@mail.com"))
            .thenReturn(List.of());
        when(userRequestRepository.existsByUsernameOrEmail("new-user", "new-user@mail.com"))
            .thenReturn(true);

        var request = validDto();
        assertThrows(EntityAlreadyExistsException.class, () -> userService.createUser(request));

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

        UserRequest result = userService.createUser(validDto());

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
        verify(userRequestRepository, times(1)).save(any(UserRequest.class));
        verify(mailService, times(1)).sendActivationMail(any(UserRequest.class));
        verifyNoMoreInteractions(userRepository, userRequestRepository, mailService);
    }

    @Test
    void itShouldThrowWhenGetByIdCalledByNonAdmin() {
        User requester = new User();
        requester.setRole(Role.ROLE_USER);

        var id = UUID.randomUUID();
        assertThrows(InvalidAccessException.class, () -> userService.getById(id, requester));

        verifyNoInteractions(userRepository, userRequestRepository);
    }

    @Test
    void itShouldThrowWhenUserNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        var admin = adminUser();
        assertThrows(InvalidDataException.class, () -> userService.getById(id, admin));

        verify(userRepository, times(1)).findById(id);
        verifyNoInteractions(userRequestRepository);
    }

    @Test
    void itShouldReturnUserWhenFound() {
        UUID id = UUID.randomUUID();
        User expected = new User();
        expected.setId(id);
        when(userRepository.findById(id)).thenReturn(Optional.of(expected));

        UserDto result = userService.getById(id, adminUser());

        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFields("imageUrl")
            .isEqualTo(Mapper.toUserDto(expected));

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
            userService,
            method,
            new Object[]{null, new UserChangeDto("new-user", null, null, null, null, null, null), adminUser()}
        );
        var dataViolations = validator.validateParameters(
            userService,
            method,
            new Object[]{UUID.randomUUID(), null, adminUser()}
        );
        var requesterViolations = validator.validateParameters(
            userService,
            method,
            new Object[]{UUID.randomUUID(), new UserChangeDto("new-user", null, null, null, null, null, null), null}
        );

        assertThat(idViolations).hasSize(1);
        assertThat(dataViolations).hasSize(1);
        assertThat(requesterViolations).hasSize(1);
    }

    @Test
    void itShouldThrowWhenNoFieldsProvided() {
        var id = UUID.randomUUID();
        var data = new UserChangeDto(null, null, null, null, null, null, null);
        var admin = adminUser();

        assertThrows(InvalidDataException.class, () -> userService.updateUser(id, data, admin));

        verifyNoInteractions(userRepository, userRequestRepository);
    }

    @Test
    void itShouldThrowWhenUpdatingNonExistingUser() {
        var id = UUID.randomUUID();
        var data = new UserChangeDto("user", null, null, null, null, null, null);
        var admin = adminUser();

        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(InvalidDataException.class, () -> userService.updateUser(id, data, admin));

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

        var data = new UserChangeDto("new-user", null, null, null, null, null, null);

        assertThrows(InvalidAccessException.class, () -> userService.updateUser(id, data, requester));

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

        var data = new UserChangeDto("new-user", null, "new@mail.com", null, null, null, null);
        var admin = adminUser();

        assertThrows(EntityAlreadyExistsException.class, () -> userService.updateUser(id, data, admin));
        
        verify(userRepository, times(1)).findById(id);
        verify(userRepository, times(1)).findActiveByUsernameOrEmail("new-user", "new@mail.com");
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(userRequestRepository);
    }
    
    @Test
    void itShouldThrowWhenUpdateUserIsRoleUserUpdatingRole() {
        var user = new User();
        user.setRole(Role.ROLE_USER);

        var id = UUID.randomUUID();
        var data = new UserChangeDto(null, null, null, Role.ROLE_ADMIN, null, null, null);

        assertThrows(InvalidAccessException.class,  () -> userService.updateUser(id, data, user));

        verifyNoInteractions(userRepository, userRequestRepository);
    }

    @Test
    void itShouldThrowWhenUpdateUserIsRoleUserUpdatingStatus() {
        var user = new User();
        user.setRole(Role.ROLE_USER);

        var id = UUID.randomUUID();
        var data = new UserChangeDto(null, null, null, null, Boolean.FALSE, null, null);

        assertThrows(InvalidAccessException.class,  () -> userService.updateUser(id, data, user));

        verifyNoInteractions(userRepository, userRequestRepository);
    }

    // ### end updateUser ###

    @Test
    void itShouldThrowWhenGetAllUsersCalledByNonAdmin() {
        User requester = new User();
        requester.setRole(Role.ROLE_USER);

        assertThrows(InvalidAccessException.class, () -> userService.getAllUsers(null, null, null, requester));

        verifyNoInteractions(userRepository, userRequestRepository);
    }

    @Test
    void itShouldThrowWhenSearchTypeWithoutTerm() {
        var admin = adminUser();
        assertThrows(InvalidDataException.class, () -> userService.getAllUsers(null, UserSearchType.USERNAME, " ", admin));
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

        var result = userService.getAllUsers(null, null, null, adminUser());

        assertThat(result).hasSize(1);
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void itShouldFilterByActive() {
        User user1 = new User(); user1.setActive(true);
        when(userRepository.findByActive(true)).thenReturn(List.of(user1));

        var result = userService.getAllUsers(true, null, null, adminUser());

        assertThat(result).hasSize(1);
        verify(userRepository, times(1)).findByActive(true);
    }

    @Test
    void itShouldFilterByUsername() {
        User user1 = new User(); user1.setUsername("john"); user1.setActive(true);
        when(userRepository.findByUsernameContainingIgnoreCase("jo")).thenReturn(List.of(user1));

        var result = userService.getAllUsers(null, UserSearchType.USERNAME, "jo", adminUser());

        assertThat(result).hasSize(1);
        verify(userRepository, times(1)).findByUsernameContainingIgnoreCase("jo");
    }

    @Test
    void itShouldFilterByEmailAndActive() {
        User user1 = new User(); user1.setEmail("mail@test.com"); user1.setActive(false);
        when(userRepository.findByActiveAndEmailContainingIgnoreCase(false, "mail")).thenReturn(List.of(user1));

        var result = userService.getAllUsers(false, UserSearchType.EMAIL, "mail", adminUser());

        assertThat(result).hasSize(1);
        verify(userRepository, times(1)).findByActiveAndEmailContainingIgnoreCase(false, "mail");
    }

    // ### activateUser ###

    @Test
    void itShouldThrowWhenActivateUserDoesntExist() {
        var id = UUID.randomUUID();

        when(userRequestRepository.findById(id))
            .thenReturn(Optional.empty());
        
        assertThrows(InvalidDataException.class, 
            () -> userService.activateUser(id)
        );

        verify(userRequestRepository, times(1)).findById(id);
        verifyNoMoreInteractions(userRequestRepository);
        verifyNoInteractions(userRepository);
    }
    
    @SuppressWarnings("null")
    @Test
    void itShouldThrowWhenIdActivateUserIsNull() {
        when(userRequestRepository.findById(null))
            .thenReturn(Optional.empty());
        
        assertThrows(InvalidDataException.class, 
            () -> userService.activateUser(null)
        );

        verify(userRequestRepository, times(1)).findById(null);
        verifyNoMoreInteractions(userRequestRepository);
        verifyNoInteractions(userRepository);
    }

    @Test
    void itShouldActivateUser() {
        var id = UUID.randomUUID();
        var encoder = new BCryptPasswordEncoder();

        var request = new UserRequest();
        request.setId(id);
        request.setDtCreated(LocalDateTime.now());
        request.setEmail(id+"@test.com");
        request.setPassword(encoder.encode(id+"-password"));
        request.setRole(Role.ROLE_ADMIN);
        request.setUsername(id+"-username");

        when(userRequestRepository.findById(id))
            .thenReturn(Optional.of(request));

        var argCaptor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(argCaptor.capture()))
            .thenAnswer(a -> {
                var user =  (User) a.getArgument(0);
                user.setId(UUID.randomUUID());
                return user;
            });

        var result = userService.activateUser(id);

        assertThat(result)
            .isNotNull()
            .satisfies(user -> {
               assertThat(user.getId()).isNotNull().isNotEqualTo(request.getId());
               assertThat(user.getUsername()).isEqualTo(request.getUsername());
               assertThat(user.getEmail()).isEqualTo(request.getEmail());
               assertThat(user.getRole()).isEqualTo(request.getRole());
               assertThat(user.isActive()).isTrue();
            });

        assertThat(argCaptor.getValue())
            .isNotNull()
            .satisfies(user -> {
               assertThat(user.getUsername()).isEqualTo(request.getUsername());
               assertThat(user.getPassword()).isEqualTo(request.getPassword());
               assertThat(user.getEmail()).isEqualTo(request.getEmail());
               assertThat(user.getRole()).isEqualTo(request.getRole());
            });

        verify(userRequestRepository, times(1)).findById(id);
        verify(userRequestRepository, times(1)).deleteById(id);
        verifyNoMoreInteractions(userRequestRepository);

        verify(userRepository, times(1)).save(any());
        verifyNoMoreInteractions(userRepository);
    }

    // ### end activateUser ###

    // ### findAllRequest ###

    @Test
    void itShouldFindAllRequest() {
        var request1 = new UserRequest();
        request1.setId(UUID.randomUUID());

        var request2 = new UserRequest();
        request2.setId(UUID.randomUUID());

        when(userRequestRepository.findAll()).thenReturn(List.of(request1, request2));

        var result = userService.findAllRequest(null, null);

        assertThat(result)
            .hasSize(2)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(Mapper.toRequestDto(request1), Mapper.toRequestDto(request2));

        verify(userRequestRepository, times(1)).findAll();
        verifyNoMoreInteractions(userRequestRepository);
        verifyNoInteractions(userRepository);
    }

    @Test
    void itShouldFindRequestByEmail() {
        var request1 = new UserRequest();
        request1.setId(UUID.randomUUID());
        request1.setEmail("test@test");

        var request2 = new UserRequest();
        request2.setId(UUID.randomUUID());
        request2.setEmail("test2@test");

        when(userRequestRepository.findAllByEmailContainingIgnoringCase("test")).thenReturn(List.of(request1, request2));

        var result = userService.findAllRequest("EMAIL", "test");

        assertThat(result)
            .hasSize(2)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(Mapper.toRequestDto(request1), Mapper.toRequestDto(request2))
            .allMatch(usr -> usr.email() != null && usr.email().toLowerCase().contains("test"));

        verify(userRequestRepository, times(1)).findAllByEmailContainingIgnoringCase("test");
        verifyNoMoreInteractions(userRequestRepository);
        verifyNoInteractions(userRepository);
    }

    @Test
    void itShouldFindRequestByUsername() {
        var request1 = new UserRequest();
        request1.setId(UUID.randomUUID());
        request1.setUsername("test");

        var request2 = new UserRequest();
        request2.setId(UUID.randomUUID());
        request2.setUsername("test2");

        when(userRequestRepository.findAllByUsernameContainingIgnoringCase("test")).thenReturn(List.of(request1, request2));

        var result = userService.findAllRequest("USERNAME", "test");

        assertThat(result)
            .hasSize(2)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(Mapper.toRequestDto(request1), Mapper.toRequestDto(request2))
            .allMatch(usr -> usr.username() != null && usr.username().toLowerCase().contains("test"));

        verify(userRequestRepository, times(1)).findAllByUsernameContainingIgnoringCase("test");
        verifyNoMoreInteractions(userRequestRepository);
        verifyNoInteractions(userRepository);
    }

    // ### end findAllRequest ###
}

