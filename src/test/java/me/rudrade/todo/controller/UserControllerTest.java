package me.rudrade.todo.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import me.rudrade.todo.config.ControllerIntegration;
import me.rudrade.todo.dto.UserDto;
import me.rudrade.todo.dto.UserRequestDto;
import me.rudrade.todo.dto.response.UsersResponse;
import me.rudrade.todo.dto.types.UserSearchType;
import me.rudrade.todo.model.User;
import me.rudrade.todo.model.types.Role;
import me.rudrade.todo.repository.UserRepository;
import me.rudrade.todo.repository.UserRequestRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.fasterxml.jackson.core.type.TypeReference;

class UserControllerTest extends ControllerIntegration {

    private static final String URI_CREATE_USER = "/todo/api/users";
    private static final String URI_GET_USER = "/todo/api/users/{id}";
    private static final String URI_DELETE_USER = "/todo/api/users/{id}";
    private static final String URI_GET_USERS = "/todo/api/users";

    @Autowired private MockMvcTester mvc;
    @Autowired private UserRequestRepository userRequestRepository;
    @Autowired private UserRepository userRepository;

    @Test
    void itShouldReturnForbiddenWhenUserIsNotAdmin() throws Exception {
        UserRequestDto request = new UserRequestDto("candidate", "secret", "candidate@mail.com", Role.ROLE_USER);

        assertThat(mvc.post().uri(URI_CREATE_USER)
            .contentType(MediaType.APPLICATION_JSON)
            .headers(getAuthHeader())
            .content(mapper().writeValueAsString(request))
        ).hasStatus(HttpStatus.FORBIDDEN);
    }

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
    void itShouldReturnUserWhenAdminRequests() throws Exception {
        User existing = getTestUser();

        assertThat(mvc.get().uri(URI_GET_USER, existing.getId())
            .headers(getAdminAuthHeader())
        ).hasStatusOk()
        .bodyJson()
        .convertTo(UserDto.class)
        .satisfies(dto -> {
            assertThat(dto.id()).isEqualTo(existing.getId());
            assertThat(dto.username()).isEqualTo(existing.getUsername());
            assertThat(dto.email()).isEqualTo(existing.getEmail());
            assertThat(dto.role()).isEqualTo(existing.getRole());
            assertThat(dto.active()).isEqualTo(existing.isActive());
        });
    }

    @Test
    void itShouldReturnBadRequestWhenUserNotFound() throws Exception {
        assertThat(mvc.get().uri(URI_GET_USER, UUID.randomUUID())
            .headers(getAdminAuthHeader())
        ).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void itShouldDeactivateUserWhenAdmin() throws Exception {
        User user = new User();
        user.setUsername("delete-user-" + UUID.randomUUID());
        user.setPassword("pass");
        user.setEmail("delete-user-" + UUID.randomUUID() + "@mail.com");
        user.setRole(Role.ROLE_USER);
        user.setActive(true);
        user = userRepository.save(user);

        assertThat(mvc.delete().uri(URI_DELETE_USER, user.getId())
            .headers(getAdminAuthHeader())
        ).hasStatusOk();

        assertThat(userRepository.findById(user.getId()))
            .isPresent()
            .get()
            .satisfies(deactivated -> assertThat(deactivated.isActive()).isFalse());
    }

    @Test
    void itShouldReturnForbiddenWhenDeactivateAsUser() throws Exception {
        User user = new User();
        user.setUsername("delete-user2-" + UUID.randomUUID());
        user.setPassword("pass");
        user.setEmail("delete-user2-" + UUID.randomUUID() + "@mail.com");
        user.setRole(Role.ROLE_USER);
        user.setActive(true);
        user = userRepository.save(user);

        assertThat(mvc.delete().uri(URI_DELETE_USER, user.getId())
            .headers(getAuthHeader())
        ).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void itShouldReturnBadRequestWhenDeactivateUserNotFound() throws Exception {
        assertThat(mvc.delete().uri(URI_DELETE_USER, UUID.randomUUID())
            .headers(getAdminAuthHeader())
        ).hasStatus(HttpStatus.BAD_REQUEST);
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

        assertThat(list.stream().anyMatch(dto -> dto.username().equals(activeUser.getUsername()))).isTrue();
        assertThat(list.stream().anyMatch(dto -> dto.username().equals(inactiveUser.getUsername()))).isTrue();
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
        assertThat(list).isNotEmpty().allSatisfy(dto -> assertThat(dto.active()).isTrue());
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
        assertThat(list).isNotEmpty().allSatisfy(dto -> assertThat(dto.username()).containsIgnoringCase("search-username"));
    }

    @Test
    void itShouldReturnForbiddenWhenListingUsersAsUser() throws Exception {
        assertThat(mvc.get().uri(URI_GET_USERS)
            .headers(getAuthHeader())
        ).hasStatus(HttpStatus.FORBIDDEN);
    }

}

