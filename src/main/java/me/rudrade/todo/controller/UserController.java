package me.rudrade.todo.controller;

import org.springframework.http.HttpHeaders;

import java.util.UUID;

import org.springframework.web.bind.annotation.*;

import lombok.AllArgsConstructor;
import me.rudrade.todo.dto.NewPasswordDto;
import me.rudrade.todo.dto.PasswordResetDto;
import me.rudrade.todo.dto.UserChangeDto;
import me.rudrade.todo.dto.UserDto;
import me.rudrade.todo.dto.UserRequestDto;
import me.rudrade.todo.dto.response.RequestListResponse;
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
        return userService.getById(id, requester);
    }

    @PatchMapping("/{id}")
    public UserDto updateUser(
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authToken,
        @PathVariable UUID id,
        @ModelAttribute UserChangeDto body
    ) {
        var requester = authenticationService.getUserByAuth(authToken);
        return userService.updateUser(id, body, requester);
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

    @GetMapping("/requests")
    public RequestListResponse getAllRequests(
        @RequestParam(required = false) String filterType,
        @RequestParam(required = false) String filterValue) {
        return new RequestListResponse(userService.findAllRequest(filterType, filterValue));
    }

    @PatchMapping("/requests/mail/{id}")
    public void resendMail(@PathVariable UUID id) {
        userService.resendMail(id);
    }

    @DeleteMapping("/requests/{id}")
    public void deleteRequest(@PathVariable UUID id) {
        userService.deleteUserRequest(id);
    }

    @PostMapping("/reset-password")
	public void resetPassword(@RequestBody PasswordResetDto body) {
        userService.resetPassword(body);
	}

    @PatchMapping("/reset-password/{id}")
    public void setNewPassword(@PathVariable UUID id, @RequestBody NewPasswordDto body) {
        userService.setNewPassword(id, body);
    }

}
