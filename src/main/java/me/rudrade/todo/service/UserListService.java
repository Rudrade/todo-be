package me.rudrade.todo.service;

import me.rudrade.todo.dto.UserListDto;
import me.rudrade.todo.model.User;
import me.rudrade.todo.model.UserList;
import me.rudrade.todo.repository.UserListRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class UserListService {

    private final UserListRepository userListRepository;
    private final AuthenticationService authenticationService;

    public UserListService(UserListRepository userListRepository, AuthenticationService authenticationService) {
        this.userListRepository = userListRepository;
        this.authenticationService = authenticationService;
    }
    public List<UserListDto> getUserListsByToken(String authToken) {
        Optional<User> optUser = authenticationService.getUserByAuth(authToken);
        if (optUser.isEmpty())
            return List.of();

        List<UserListDto> result = new ArrayList<>();
        optUser.get().getUserLists().forEach(lst ->
            result.add(new UserListDto(lst.getName(), lst.getColor(), lst.getTasks() == null ? 0 : lst.getTasks().size()))
        );

        return result;
    }

    public UserList saveByName(String listName, User user) {
        Optional<UserList> optList = userListRepository.findByName(listName);
        String color = generateRandomHexColor();

        return optList.orElseGet(() -> userListRepository.save(new UserList(null, listName, color, user, null)));
    }

    private String generateRandomHexColor() {
        int nextInt = ThreadLocalRandom.current().nextInt(0xffffff + 1);
        return String.format("#%06x", nextInt);
    }

    public Optional<UserList> findByName(String name) {
        return userListRepository.findByName(name);
    }
}
