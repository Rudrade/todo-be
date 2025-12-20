package me.rudrade.todo.service;

import me.rudrade.todo.model.User;
import me.rudrade.todo.model.UserList;
import me.rudrade.todo.repository.UserListRepository;
import me.rudrade.todo.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserListServiceTest {

    @Mock private JwtService jwtService;
    @Mock private UserRepository userRepository;
    @Mock private UserListRepository userListRepository;
    @Mock private AuthenticationService authenticationService;

    private UserListService userListService;
    private UserListService getUserService() {
        if (userListService == null) {
            userListService = new UserListService(userListRepository, authenticationService);
        }
        return userListService;
    }

    @Test
    void itShouldGetUserListByToken() {
        UserList list1 = new UserList();
        list1.setId(UUID.randomUUID());
        list1.setName("First");

        UserList list2 = new UserList();
        list2.setId(UUID.randomUUID());
        list2.setName("Name");

        User user = new User();
        user.setUsername("user-test");
        user.setUserLists(List.of(list1, list2));

        when(authenticationService.getUserByAuth("token-test"))
            .thenReturn(Optional.of(user));

        List<String> result = getUserService().getUserListsByToken("token-test");

        assertThat(result)
            .hasSize(2)
            .containsExactlyInAnyOrder("First", "Name");
    }

    @Test
    void itShouldGetEmptyUserListByTokenWhenUserIsNotFound() {
        when(authenticationService.getUserByAuth("token-test"))
            .thenReturn(Optional.empty());

        List<String> result = getUserService().getUserListsByToken("token-test");

        assertThat(result).isEmpty();
    }

    @Test
    void itShouldSaveByName() {
        when(userListRepository.findByName("test-list"))
            .thenReturn(Optional.empty());

        User user = new User();

        when(userListRepository.save(any()))
            .thenReturn(new UserList(UUID.randomUUID(), "test-list", user, null));

        UserList result = getUserService().saveByName("test-list", user);

        assertThat(result)
            .isNotNull()
            .satisfies(lst -> {
               assertThat(lst.getId()).isNotNull();
               assertThat(lst.getName()).isEqualTo("test-list");
            });

        verify(userListRepository, times(1)).findByName("test-list");
        verify(userListRepository, times(1)).save(any(UserList.class));
        verifyNoMoreInteractions(userListRepository);
    }

    @Test
    void itShouldNotSaveNewByName() {
        UserList list = new UserList();
        list.setId(UUID.randomUUID());
        list.setName("test-list");

        when(userListRepository.findByName("test-list"))
            .thenReturn(Optional.of(list));

        User user = new User();

        UserList result = getUserService().saveByName("test-list", user);

        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFields("user", "tasks")
            .isEqualTo(list);

        verify(userListRepository, times(1)).findByName("test-list");
        verifyNoMoreInteractions(userListRepository);
    }

}
