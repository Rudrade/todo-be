package me.rudrade.todo.service;

import me.rudrade.todo.dto.Mapper;
import me.rudrade.todo.dto.UserListDto;
import me.rudrade.todo.exception.InvalidAccessException;
import me.rudrade.todo.exception.InvalidDataException;
import me.rudrade.todo.model.User;
import me.rudrade.todo.model.UserList;
import me.rudrade.todo.repository.UserListRepository;
import me.rudrade.todo.util.ServiceUtil;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserListService extends ServiceUtil {

    private final UserListRepository userListRepository;
    private final AuthenticationService authenticationService;

    public UserListService(UserListRepository userListRepository, AuthenticationService authenticationService) {
        this.userListRepository = userListRepository;
        this.authenticationService = authenticationService;
    }
    public List<UserListDto> getUserListsByToken(String authToken) {
        if (authToken == null || authToken.isBlank())
            throw new InvalidAccessException();

        User user = authenticationService.getUserByAuth(authToken);

        return user.getUserLists().stream().map(Mapper::toListDto).toList();
    }

    public UserList saveByName(String listName, User user) {
        if (user == null)
            throw new InvalidAccessException();

        if (listName == null || listName.isBlank())
            throw new InvalidDataException("List name must be filled.");

        Optional<UserList> optList = findByName(listName, user);
        return optList.orElseGet(() -> userListRepository.save(new UserList(null, listName, generateRandomHexColor(), user, null)));
    }

    public Optional<UserList> findByName(String name, User user) {
        if (name == null || name.isBlank() || user == null || user.getId() == null)
            throw new InvalidAccessException();

        return userListRepository.findByNameAndUserId(name, user.getId());
    }
}
