package me.rudrade.todo.service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import me.rudrade.todo.dto.Mapper;
import me.rudrade.todo.dto.UserDto;
import me.rudrade.todo.dto.UserRequestDto;
import me.rudrade.todo.dto.types.UserSearchType;
import me.rudrade.todo.exception.EntityAlreadyExistsException;
import me.rudrade.todo.exception.InvalidAccessException;
import me.rudrade.todo.exception.InvalidDataException;
import me.rudrade.todo.model.User;
import me.rudrade.todo.model.UserRequest;
import me.rudrade.todo.model.types.Role;
import me.rudrade.todo.repository.UserRepository;
import me.rudrade.todo.repository.UserRequestRepository;
import me.rudrade.todo.util.ServiceUtil;

@Service
@Validated
@AllArgsConstructor
public class UserService extends ServiceUtil {

    private final UserRepository userRepository;
    private final UserRequestRepository userRequestRepository;

    public UserRequest createUser(
        @NotNull UserRequestDto request,
        @NotNull User createdBy
    ) {
        if (!Role.ROLE_ADMIN.equals(createdBy.getRole()))
            throw new InvalidAccessException();

        var user = Mapper.toUserRequest(request);
        user.setDtCreated(LocalDateTime.now());
        validate(user);

        var existsUser = userRepository.findActiveByUsernameOrEmail(user.getUsername(), user.getEmail());
        if (existsUser.isPresent())
            throw new EntityAlreadyExistsException("User with the same username and/or email already exists.");

        var existsInRequest = userRequestRepository.existsByUsernameOrEmail(user.getUsername(), user.getEmail());
        if (existsInRequest)
            throw new EntityAlreadyExistsException("User with the same username and/or email already exists. Please check your email for activation.");

        userRequestRepository.save(user);

        return user;
    }

    public User getById(
        @NotNull(message = "User id must be provided.") UUID id,
        @NotNull User requester
    ) {
        if (!Role.ROLE_ADMIN.equals(requester.getRole()))
            throw new InvalidAccessException();
        
        return userRepository.findById(id)
            .orElseThrow(() -> new InvalidDataException("User not found."));
    }

    public void deactivateById(
        @NotNull(message = "User id must be provided.") UUID id,
        @NotNull User requester
    ) {
        if (!Role.ROLE_ADMIN.equals(requester.getRole()))
            throw new InvalidAccessException();

        User user = userRepository.findById(id)
            .orElseThrow(() -> new InvalidDataException("User not found."));

        if (user.isActive()) {
            user.setActive(false);
            userRepository.save(user);
        }
    }

    public List<UserDto> getAllUsers(
        Boolean active,
        UserSearchType searchType,
        String searchTerm,
        @NotNull User requester
    ) {
        if (!Role.ROLE_ADMIN.equals(requester.getRole()))
            throw new InvalidAccessException();

        if (searchType != null && (searchTerm == null || searchTerm.isBlank()))
            throw new InvalidDataException("Search term must be provided when search type is set.");

        List<User> users = new ArrayList<>();

        if (searchType != null && active != null) {
            if (searchType == UserSearchType.USERNAME) {
                users.addAll(userRepository.findByActiveAndUsernameContainingIgnoreCase(active, searchTerm));
            } else {
                users.addAll(userRepository.findByActiveAndEmailContainingIgnoreCase(active, searchTerm));
            }
        } else if (searchType != null) {
            if (searchType == UserSearchType.USERNAME) {
                users.addAll(userRepository.findByUsernameContainingIgnoreCase(searchTerm));
            } else {
                users.addAll(userRepository.findByEmailContainingIgnoreCase(searchTerm));
            }
        } else if (active != null) {
            users.addAll(userRepository.findByActive(active));
        } else {
            userRepository.findAll().forEach(users::add);
        }

        return users.stream()
            .map(Mapper::toUserDto)
            .toList();
    }

}
