package me.rudrade.todo.controller;

import org.springframework.http.HttpHeaders;

import java.util.UUID;

import org.springframework.web.bind.annotation.*;

import lombok.AllArgsConstructor;
import me.rudrade.todo.dto.Mapper;
import me.rudrade.todo.dto.UserChangeDto;
import me.rudrade.todo.dto.UserDto;
import me.rudrade.todo.dto.UserRequestDto;
import me.rudrade.todo.dto.response.UsersResponse;
import me.rudrade.todo.dto.types.UserSearchType;
import me.rudrade.todo.service.AuthenticationService;
import me.rudrade.todo.service.UserService;

@RestController
@RequestMapping("/todo/api/users")
@AllArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    public void createUser(@RequestBody UserRequestDto body) {
        
        userService.createUser(body);
    }

    @GetMapping("/{id}")
    public UserDto getUser(
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authToken,
        @PathVariable UUID id
    ) {
        var requester = authenticationService.getUserByAuth(authToken);
        return Mapper.toUserDto(userService.getById(id, requester));
    }

    @PatchMapping("/{id}")
    public UserDto updateUser(
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authToken,
        @PathVariable UUID id,
        @RequestBody UserChangeDto body
    ) {
        var requester = authenticationService.getUserByAuth(authToken);
        var result = userService.updateUser(id, body, requester);

        return Mapper.toUserDto(result);
    }

    @GetMapping()
    public UsersResponse getUsers(
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authToken,
        @RequestParam(required = false) Boolean active,
        @RequestParam(required = false) UserSearchType searchType,
        @RequestParam(required = false) String searchTerm
    ) {
        var requester = authenticationService.getUserByAuth(authToken);
        return new UsersResponse(userService.getAllUsers(active, searchType, searchTerm, requester));
    }

    @PostMapping("/activate/{id}")
    public UserDto activateUser(@PathVariable UUID id) {
        return userService.activateUser(id);
    }


}
