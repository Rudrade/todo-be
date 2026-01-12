package me.rudrade.todo.service;

import me.rudrade.todo.dto.UserListDto;
import me.rudrade.todo.exception.InvalidAccessException;
import me.rudrade.todo.exception.InvalidDataException;
import me.rudrade.todo.model.User;
import me.rudrade.todo.model.UserList;
import me.rudrade.todo.repository.UserListRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserListServiceTest {

    @Mock private UserListRepository userListRepository;
    @Mock private AuthenticationService authenticationService;
    @Mock private MessageSource messageSource;

    private UserListService userListService;
    private UserListService getUserService() {
        if (userListService == null) {
            userListService = new UserListService(userListRepository, authenticationService, messageSource);
        }
        return userListService;
    }

    @Test
    void itShouldGetUserListByToken() {
        UserList list1 = new UserList(UUID.randomUUID(), "First", "red", null, null);
        UserList list2 = new UserList(UUID.randomUUID(), "Name", "blue", null, null);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("user-test");
        user.setUserLists(List.of(list1, list2));

        when(authenticationService.getUserByAuth("token-test"))
            .thenReturn(user);

        List<UserListDto> result = getUserService().getUserListsByToken("token-test");

        assertThat(result)
            .hasSize(2)
            .extracting(UserListDto::name)
            .containsExactlyInAnyOrder("First", "Name");

        verify(authenticationService, times(1)).getUserByAuth("token-test");
        verifyNoMoreInteractions(authenticationService);
        verifyNoInteractions(userListRepository);
    }

    @Test
    void itShouldThrowWhenTokenIsNull() {
        UserListService service = getUserService();

        assertThrows(InvalidAccessException.class, () -> service.getUserListsByToken(null));

        verifyNoInteractions(authenticationService, userListRepository);
    }

    @Test
    void itShouldThrowWhenTokenIsBlank() {
        UserListService service = getUserService();

        assertThrows(InvalidAccessException.class, () -> service.getUserListsByToken("   "));

        verifyNoInteractions(authenticationService, userListRepository);
    }

    @Test
    void itShouldSaveByNameWhenNotExists() {
        User user = new User();
        user.setId(UUID.randomUUID());

        when(userListRepository.findByNameAndUserId("test-list", user.getId()))
            .thenReturn(Optional.empty());
        when(userListRepository.save(any(UserList.class)))
            .thenAnswer(invocation -> {
                UserList saved = invocation.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });

        UserList result = getUserService().saveByName("test-list", user);

        assertThat(result)
            .isNotNull()
            .satisfies(lst -> {
               assertThat(lst.getId()).isNotNull();
               assertThat(lst.getName()).isEqualTo("test-list");
               assertThat(lst.getColor()).isNotBlank();
               assertThat(lst.getUser()).isEqualTo(user);
            });

        verify(userListRepository, times(1)).findByNameAndUserId("test-list", user.getId());
        verify(userListRepository, times(1)).save(any(UserList.class));
        verifyNoMoreInteractions(userListRepository);
        verifyNoInteractions(authenticationService);
    }

    @Test
    void itShouldReturnExistingWhenSaveByNameFindsOne() {
        User user = new User();
        user.setId(UUID.randomUUID());
        UserList existing = new UserList(UUID.randomUUID(), "test-list", "red", user, null);

        when(userListRepository.findByNameAndUserId("test-list", user.getId()))
            .thenReturn(Optional.of(existing));

        UserList result = getUserService().saveByName("test-list", user);

        assertThat(result).isEqualTo(existing);

        verify(userListRepository, times(1)).findByNameAndUserId("test-list", user.getId());
        verifyNoMoreInteractions(userListRepository);
        verifyNoInteractions(authenticationService);
    }

    @Test
    void itShouldThrowWhenSaveByNameWithNullUser() {
        UserListService service = getUserService();

        assertThrows(InvalidAccessException.class, () -> service.saveByName("name", null));

        verifyNoInteractions(userListRepository, authenticationService);
    }

    @Test
    void itShouldThrowWhenSaveByNameWithBlankName() {
        User user = new User();
        user.setId(UUID.randomUUID());
        UserListService service = getUserService();

        assertThrows(InvalidDataException.class, () -> service.saveByName("   ", user));

        verifyNoInteractions(userListRepository, authenticationService);
    }

    @Test
    void itShouldThrowWhenSaveByNameWithNullName() {
        User user = new User();
        user.setId(UUID.randomUUID());
        UserListService service = getUserService();

        assertThrows(InvalidDataException.class, () -> service.saveByName(null, user));

        verifyNoInteractions(userListRepository, authenticationService);
    }

    @Test
    void itShouldFindByName() {
        User user = new User();
        user.setId(UUID.randomUUID());
        UserList list = new UserList(UUID.randomUUID(), "Test", "red", user, null);

        when(userListRepository.findByNameAndUserId("Test", user.getId()))
            .thenReturn(Optional.of(list));

        Optional<UserList> result = getUserService().findByName("Test", user);

        assertThat(result).contains(list);
        verify(userListRepository, times(1)).findByNameAndUserId("Test", user.getId());
        verifyNoMoreInteractions(userListRepository);
        verifyNoInteractions(authenticationService);
    }

    @Test
    void itShouldThrowWhenFindByNameWithInvalidArgs() {
        User validUser = new User();
        validUser.setId(UUID.randomUUID());
        User userWithoutId = new User();
        UserListService service = getUserService();

        assertThrows(InvalidAccessException.class, () -> service.findByName(null, validUser));
        assertThrows(InvalidAccessException.class, () -> service.findByName("   ", validUser));
        assertThrows(InvalidAccessException.class, () -> service.findByName("name", null));
        assertThrows(InvalidAccessException.class, () -> service.findByName("name", userWithoutId));

        verifyNoInteractions(userListRepository, authenticationService);
    }

}
