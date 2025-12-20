package me.rudrade.todo.service;

import me.rudrade.todo.model.User;
import me.rudrade.todo.model.UserList;
import me.rudrade.todo.repository.UserListRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserListService {

    private final UserListRepository userListRepository;
    private final AuthenticationService authenticationService;

    public UserListService(UserListRepository userListRepository, AuthenticationService authenticationService) {
        this.userListRepository = userListRepository;
        this.authenticationService = authenticationService;
    }
    public List<String> getUserListsByToken(String authToken) {
        Optional<User> optUser = authenticationService.getUserByAuth(authToken);
        return optUser.map(user -> user.getUserLists().stream()
            .map(UserList::getName)
            .toList())
            .orElseGet(ArrayList::new);
    }

    public UserList saveByName(String listName, User user) {
        Optional<UserList> optList = userListRepository.findByName(listName);
        return optList.orElseGet(() -> userListRepository.save(new UserList(null, listName, user, null)));
    }
}
