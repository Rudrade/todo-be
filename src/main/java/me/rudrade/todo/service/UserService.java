package me.rudrade.todo.service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import me.rudrade.todo.dto.Mapper;
import me.rudrade.todo.dto.UserChangeDto;
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

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final UserRequestRepository userRequestRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    public UserRequest createUser(@NotNull UserRequestDto request) {
        var user = Mapper.toUserRequest(request);
        user.setDtCreated(LocalDateTime.now());
        validate(user);

        validateAlreadyExists(user.getUsername(), user.getEmail(), null);

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        userRequestRepository.save(user);

        // Send activation mail and mark mail as sent
        try {
            mailService.sendActivationMail(user);

            user.setMailSent(true);
            userRequestRepository.save(user);

        } catch (MessagingException e) {
            logger.error("Error sending mail", e);
        }

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

    public User updateUser(
        @NotNull(message = "User id must be provided.") UUID id,
        @NotNull UserChangeDto data,
        @NotNull User requester
    ) {
        // Validate that at least one property is being updated
        if (data.username() == null &&
            data.password() == null &&
            data.email() == null &&
            data.role() == null &&
            data.active() == null) {
            throw new InvalidDataException("A property must be set to update the resource.");
        }

        // Only admins can update role or active properties
        if (!Role.ROLE_ADMIN.equals(requester.getRole()) && (data.role() != null || data.active() != null))
            throw new InvalidAccessException();

        // Validate if resource exists
        User user = userRepository.findById(id)
            .orElseThrow(() -> new InvalidDataException("User not found."));

        // Validate if is either is admin or is own-user
        if (!Role.ROLE_ADMIN.equals(requester.getRole()) && !id.equals(requester.getId())) {
            throw new InvalidAccessException();
        }

        // If changing username or email, validate if already doesn't exist on in DB
        validateAlreadyExists(data.username(), data.email(), id);

        // Update only setted values
        if (data.username() != null) {
            user.setUsername(data.username());
        }

        if (data.password() != null) {
            user.setPassword(passwordEncoder.encode(data.password()));
        }

        if (data.email() != null) {
            user.setEmail(data.email());
        }

        if (data.role() != null) {
            user.setRole(data.role());
        }

        if (data.active() != null) {
            user.setActive(data.active());
        }

        // Save resource
        return userRepository.save(user);
    }

    private void validateAlreadyExists(String username, String email, UUID id) {
        var existsUser = userRepository.findActiveByUsernameOrEmail(username, email);
        if (!existsUser.isEmpty()) {
            var allIdMatches = id == null ? false : existsUser.stream()
                .allMatch(user -> user.getId().equals(id));
            if (!allIdMatches) {
                throw new EntityAlreadyExistsException("User with the same username and/or email already exists.");
            }
        }

        var existsInRequest = userRequestRepository.existsByUsernameOrEmail(username, email);
        if (existsInRequest)
            throw new EntityAlreadyExistsException("User with the same username and/or email already exists. Please check your email for activation.");
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

    public UserDto activateUser(UUID id) {
        // Find if request exists
        var optRequest = userRequestRepository.findById(id);
        if (optRequest.isEmpty())
            throw new InvalidDataException("User request doesn't exist");

        // Copy request to new user
        var request = optRequest.get();
        var user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setEmail(request.getEmail());
        user.setRole(request.getRole());
        user.setActive(true);

        // Insert user
        userRepository.save(user);

        // delete the request
        userRequestRepository.deleteById(id);

        // return the new user
        return Mapper.toUserDto(user);
    }

}
