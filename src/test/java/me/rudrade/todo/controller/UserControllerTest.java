package me.rudrade.todo.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;

import me.rudrade.todo.config.ControllerIntegration;
import me.rudrade.todo.dto.Mapper;
import me.rudrade.todo.dto.NewPasswordDto;
import me.rudrade.todo.dto.PasswordResetDto;
import me.rudrade.todo.dto.UserChangeDto;
import me.rudrade.todo.dto.UserDto;
import me.rudrade.todo.dto.UserRequestDto;
import me.rudrade.todo.dto.response.RequestListResponse;
import me.rudrade.todo.dto.response.UsersResponse;
import me.rudrade.todo.dto.types.UserSearchType;
import me.rudrade.todo.model.PasswordRequest;
import me.rudrade.todo.model.User;
import me.rudrade.todo.model.UserRequest;
import me.rudrade.todo.model.types.Role;
import me.rudrade.todo.repository.PasswordRequestRepository;
import me.rudrade.todo.repository.UserRepository;
import me.rudrade.todo.repository.UserRequestRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.fasterxml.jackson.core.type.TypeReference;

class UserControllerTest extends ControllerIntegration {

    private static final String URI_CREATE_USER = "/todo/api/users/register";
    private static final String URI_GET_USER = "/todo/api/users/{id}";
    private static final String URI_UPDATE_USER = "/todo/api/users/{id}";
    private static final String URI_GET_USERS = "/todo/api/users";
    private static final String URI_ACTIVATE_USER = "/todo/api/users/activate/{id}";

    @Autowired private MockMvcTester mvc;
    @Autowired private UserRequestRepository userRequestRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordRequestRepository passwordRequestRepository;

    @Test
    void itShouldCreateUserRequestWhenAdmin() throws Exception {
        String username = "candidate-" + UUID.randomUUID();
        String email = "candidate-" + UUID.randomUUID() + "@mail.com";
        UserRequestDto request = new UserRequestDto(username, "secret", email, Role.ROLE_USER);

        assertThat(mvc.post().uri(URI_CREATE_USER)
            .contentType(MediaType.APPLICATION_JSON)
            .headers(getAdminAuthHeader())
            .content(mapper().writeValueAsString(request))
        ).hasStatusOk();

        assertThat(userRequestRepository.existsByUsernameOrEmail(username, email)).isTrue();
    }

    @Test
    void itShouldReturnConflictWhenUserAlreadyExists() throws Exception {
        User existing = getTestUser();
        UserRequestDto request = new UserRequestDto(existing.getUsername(), "secret", existing.getEmail(), Role.ROLE_USER);

        assertThat(mvc.post().uri(URI_CREATE_USER)
            .contentType(MediaType.APPLICATION_JSON)
            .headers(getAdminAuthHeader())
            .content(mapper().writeValueAsString(request))
        ).hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void itShouldReturnUserWhenAdminRequests() {
        User existing = getTestUser();

        assertThat(mvc.get().uri(URI_GET_USER, existing.getId())
            .headers(getAdminAuthHeader())
        ).hasStatusOk()
        .bodyJson()
        .convertTo(UserDto.class)
        .satisfies(dto -> {
            assertThat(dto.getId()).isEqualTo(existing.getId());
            assertThat(dto.getUsername()).isEqualTo(existing.getUsername());
            assertThat(dto.getEmail()).isEqualTo(existing.getEmail());
            assertThat(dto.getRole()).isEqualTo(existing.getRole());
            assertThat(dto.isActive()).isEqualTo(existing.isActive());
        });
    }

    @Test
    void itShouldReturnBadRequestWhenUserNotFound() {
        assertThat(mvc.get().uri(URI_GET_USER, UUID.randomUUID())
            .headers(getAdminAuthHeader())
        ).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void itShouldUpdateUserWhenAdmin() {
        var user = new User();
        user.setUsername("test-"+UUID.randomUUID());
        user.setPassword("test-password");
        user.setEmail(UUID.randomUUID()+"@mail.com");
        user.setRole(Role.ROLE_USER);
        user.setActive(true);
        userRepository.save(user);

        var updatedUser = new UserChangeDto(
            "changed-"+UUID.randomUUID(),
            null,
            UUID.randomUUID()+"@mail.com",
            Role.ROLE_ADMIN,
            Boolean.FALSE,
            null, null);

        assertThat(
            mvc.patch().uri(URI_UPDATE_USER, user.getId())
                .headers(getAdminAuthHeader())
                .contentType(MEDIA_TYPE_FORM_DATA)
                .formFields(convertToFormData(updatedUser))
        ).hasStatusOk()
        .bodyJson()
        .convertTo(UserDto.class)
        .satisfies(dto -> {
            assertThat(dto.getUsername()).isEqualTo(updatedUser.getUsername());
            assertThat(dto.getEmail()).isEqualTo(updatedUser.getEmail());
            assertThat(dto.getRole()).isEqualTo(updatedUser.getRole());
            assertThat(dto.isActive()).isEqualTo(updatedUser.getActive());
        });
    }

    @Test
    void itShouldUpdateUserWhenIsOwnUser() {
        var user = new User();
        user.setUsername("test-"+UUID.randomUUID());
        user.setPassword(encoder.encode("test-password"));
        user.setEmail(UUID.randomUUID()+"@mail.com");
        user.setRole(Role.ROLE_USER);
        user.setActive(true);
        userRepository.save(user);

        var updatedUser = new UserChangeDto(
            "changed-"+UUID.randomUUID(),
            "changed-password",
            UUID.randomUUID()+"@mail.com",
            null,
            null,
            "test-password", null);

        assertThat(
            mvc.patch().uri(URI_UPDATE_USER, user.getId())
                .headers(getAuthHeader(user.getUsername(), "test-password"))
                .contentType(MEDIA_TYPE_FORM_DATA)
                .formFields(convertToFormData(updatedUser))
        ).hasStatusOk()
        .bodyJson()
        .convertTo(UserDto.class)
        .satisfies(dto -> {
            assertThat(dto.getUsername()).isEqualTo(updatedUser.getUsername());
            assertThat(dto.getEmail()).isEqualTo(updatedUser.getEmail());
            assertThat(dto.getRole()).isEqualTo(user.getRole());
            assertThat(dto.isActive()).isEqualTo(user.isActive());
        });
    }

    @Test
    void itShouldListUsersForAdmin() throws Exception {
        User activeUser = new User();
        activeUser.setUsername("list-user-" + UUID.randomUUID());
        activeUser.setPassword("pass");
        activeUser.setEmail("list-user-" + UUID.randomUUID() + "@mail.com");
        activeUser.setRole(Role.ROLE_USER);
        activeUser.setActive(true);

        User inactiveUser = new User();
        inactiveUser.setUsername("list-inactive-" + UUID.randomUUID());
        inactiveUser.setPassword("pass");
        inactiveUser.setEmail("list-inactive-" + UUID.randomUUID() + "@mail.com");
        inactiveUser.setRole(Role.ROLE_USER);
        inactiveUser.setActive(false);

        userRepository.save(activeUser);
        userRepository.save(inactiveUser);

        var response = mvc.get().uri(URI_GET_USERS)
            .headers(getAdminAuthHeader())
            .exchange()
            .getResponse()
            .getContentAsString();

        var list = mapper().readValue(response, new TypeReference<UsersResponse>() {}).users();

        assertThat(list.stream().anyMatch(dto -> dto.getUsername().equals(activeUser.getUsername()))).isTrue();
        assertThat(list.stream().anyMatch(dto -> dto.getUsername().equals(inactiveUser.getUsername()))).isTrue();
    }

    @Test
    void itShouldSaveUserWithoutChangingValues() {
        var user = new User();
        user.setActive(true);
        user.setUsername(UUID.randomUUID().toString());
        user.setEmail(user.getUsername()+"@t.com");
        user.setPassword(encoder.encode("test"));
        user.setRole(Role.ROLE_USER);
        userRepository.save(user);

        var updateData = new UserChangeDto(
            user.getUsername(),
            null,
            user.getEmail(),
            user.getRole(),
            user.isActive(),
            null, null
        );

        assertThat(
            mvc.patch().uri(URI_UPDATE_USER, user.getId())
                .headers(getAdminAuthHeader())
                .contentType(MEDIA_TYPE_FORM_DATA)
                .formFields(convertToFormData(updateData))
        ).hasStatusOk();

        var savedUser = userRepository.findById(user.getId());
        assertThat(savedUser).isNotEmpty();
        assertThat(savedUser.get())
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "username", "email", "role", "active")
            .isEqualTo(user);

        var matches = new BCryptPasswordEncoder().matches("test", savedUser.get().getPassword());
        assertThat(matches).isTrue();
    }

    @Test
    void itShouldThrowErrorWhenUserTryingToUpdateAnother() {
        var user = createUser();
        userRepository.save(user);

        var updateData = new UserChangeDto(
            null,
            "test-2",
            null,
            null,
            null,
            null, null
        );

        assertThat(
            mvc.patch().uri(URI_UPDATE_USER, user.getId())
            .headers(getAuthHeader())
            .contentType(MEDIA_TYPE_FORM_DATA)
            .formFields(convertToFormData(updateData))
        ).hasStatus(HttpStatus.FORBIDDEN);
        
    }

    @Test
    void itShouldFilterUsersByActive() throws Exception {
        User activeUser = new User();
        activeUser.setUsername("filter-active-" + UUID.randomUUID());
        activeUser.setPassword("pass");
        activeUser.setEmail("filter-active-" + UUID.randomUUID() + "@mail.com");
        activeUser.setRole(Role.ROLE_USER);
        activeUser.setActive(true);
        userRepository.save(activeUser);

        var response = mvc.get().uri(URI_GET_USERS)
            .headers(getAdminAuthHeader())
            .queryParam("active", "true")
            .exchange()
            .getResponse()
            .getContentAsString();

        var list = mapper().readValue(response, new TypeReference<UsersResponse>() {}).users();
        assertThat(list).isNotEmpty().allSatisfy(dto -> assertThat(dto.isActive()).isTrue());
    }

    @Test
    void itShouldFilterUsersByUsername() throws Exception {
        User user = new User();
        user.setUsername("search-username-" + UUID.randomUUID());
        user.setPassword("pass");
        user.setEmail("search-username-" + UUID.randomUUID() + "@mail.com");
        user.setRole(Role.ROLE_USER);
        user.setActive(true);
        userRepository.save(user);

        var response = mvc.get().uri(URI_GET_USERS)
            .headers(getAdminAuthHeader())
            .queryParam("searchType", UserSearchType.USERNAME.name())
            .queryParam("searchTerm", "search-username")
            .exchange()
            .getResponse()
            .getContentAsString();

        var list = mapper().readValue(response, new TypeReference<UsersResponse>() {}).users();
        assertThat(list).isNotEmpty().allSatisfy(dto -> assertThat(dto.getUsername()).containsIgnoringCase("search-username"));
    }

    @Test
    void itShouldReturnForbiddenWhenListingUsersAsUser() {
        assertThat(mvc.get().uri(URI_GET_USERS)
            .headers(getAuthHeader())
        ).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void itShouldActivateUser() {
        var request = new UserRequest();
        request.setUsername("request-user");
        request.setPassword(encoder.encode("request-pass"));
        request.setEmail("request-mail@test");
        request.setRole(Role.ROLE_USER);
        request.setDtCreated(LocalDateTime.now());
        userRequestRepository.save(request);

        assertThat(mvc.post().uri(URI_ACTIVATE_USER, request.getId()))
            .hasStatusOk();

        var dbRequest = userRequestRepository.findById(request.getId());
        assertThat(dbRequest).isEmpty();

        var dbUser =  userRepository.findByUsername(request.getUsername());
        assertThat(dbUser)
            .isPresent()
            .satisfies(opt -> {
               var user = opt.get();
               assertThat(user.getUsername()).isEqualTo(request.getUsername());
               assertThat(user.getPassword()).isEqualTo(request.getPassword());
               assertThat(user.getEmail()).isEqualTo(request.getEmail());
               assertThat(user.getRole()).isEqualTo(request.getRole());
               assertThat(user.getId()).isNotNull().isNotEqualTo(request.getId());
               assertThat(user.isActive()).isTrue();
            });
    }

    @Test
    void itShouldGetAllRequests() {
        userRequestRepository.deleteAll();

        var request1 = new UserRequest();
        request1.setUsername("test-request-1");
        request1.setDtCreated(LocalDateTime.now());
        request1.setEmail(request1.getUsername()+"@test");
        request1.setPassword("test");
        request1.setRole(Role.ROLE_ADMIN);
        userRequestRepository.save(request1);

        var request2 = new UserRequest();
        request2.setUsername("test-request-2");
        request2.setDtCreated(LocalDateTime.now().minusDays(5L));
        request2.setEmail(request2.getUsername()+"@test");
        request2.setPassword("test");
        request2.setRole(Role.ROLE_USER);
        userRequestRepository.save(request2);

        assertThat(mvc.get().uri("/todo/api/users/requests")
            .headers(getAdminAuthHeader())
        ).hasStatusOk()
        .bodyJson()
        .convertTo(RequestListResponse.class)
        .satisfies(res -> {
            assertThat(res.requests())
                .isNotNull()
                .hasSize(2)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("dtCreated")
                .containsExactlyInAnyOrder(Mapper.toRequestDto(request1), Mapper.toRequestDto(request2));
        });
    }

    @Test
    void itShouldResendMail() {
        var userRequest = new UserRequest();
        userRequest.setDtCreated(LocalDateTime.now());
        userRequest.setEmail("test-resend@test");
        userRequest.setMailSent(false);
        userRequest.setPassword("test");
        userRequest.setRole(Role.ROLE_USER);
        userRequest.setUsername("test-resend");
        userRequestRepository.save(userRequest);

        assertThat(mvc.patch().uri("/todo/api/users/requests/mail/"+userRequest.getId())
            .headers(getAdminAuthHeader())
        ).hasStatusOk();
        
        var dbRequest = userRequestRepository.findById(userRequest.getId());
        assertThat(dbRequest.get().isMailSent()).isTrue();
    }

    @Test
    void itShouldDeleteRequest() {
        var userRequest = new UserRequest();
        userRequest.setDtCreated(LocalDateTime.now());
        userRequest.setEmail("test-delete@test");
        userRequest.setPassword("test");
        userRequest.setRole(Role.ROLE_USER);
        userRequest.setUsername("test-delete");
        userRequestRepository.save(userRequest);

        assertThat(mvc.delete().uri("/todo/api/users/requests/"+userRequest.getId())
            .headers(getAdminAuthHeader())
        ).hasStatusOk();

        var dbRequest = userRequestRepository.findById(userRequest.getId());
        assertThat(dbRequest).isEmpty();
    }

    @Test
    void itShouldResetPassword() throws Exception {
        var user = new User();
        user.setActive(true);
        user.setEmail("reset-p@test");
        user.setPassword("test");
        user.setRole(Role.ROLE_USER);
        user.setUsername("reset-p");
        userRepository.save(user);

        assertThat(mvc.post().uri("/todo/api/users/reset-password")
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper().writeValueAsString(new PasswordResetDto(user.getUsername(), user.getEmail())))
        ).hasStatusOk();

        var lst = passwordRequestRepository.findAll();
        assertThat(lst)
            .isNotEmpty()
            .map(passReq -> passReq.getUser().getId())
            .containsExactlyInAnyOrder(user.getId());
    }

    @Test
    void itShouldSetNewPassword() throws Exception {
        var user = new User();
        user.setActive(true);
        user.setEmail("test-change@test");
        user.setPassword(encoder.encode("test"));
        user.setRole(Role.ROLE_USER);
        user.setUsername("test-change");
        userRepository.save(user);

        var request = new PasswordRequest();
        request.setDtCreated(LocalDateTime.now());
        request.setMailSent(true);
        request.setUser(user);
        passwordRequestRepository.save(request);

        assertThat(mvc.patch().uri("/todo/api/users/reset-password/"+request.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper().writeValueAsString(new NewPasswordDto("changed")))
        ).hasStatusOk();

        var dbUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(dbUser.getPassword()).isNotEqualTo(user.getPassword());
        assertThat(encoder.matches("changed", dbUser.getPassword())).isTrue();

        var dbRequest = passwordRequestRepository.findById(request.getId());
        assertThat(dbRequest).isEmpty();
    }

}

