package me.rudrade.todo.service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import me.rudrade.todo.dto.Mapper;
import me.rudrade.todo.dto.NewPasswordDto;
import me.rudrade.todo.dto.PasswordResetDto;
import me.rudrade.todo.dto.RequestDto;
import me.rudrade.todo.dto.UserChangeDto;
import me.rudrade.todo.dto.UserDto;
import me.rudrade.todo.dto.UserRequestDto;
import me.rudrade.todo.dto.types.UserSearchType;
import me.rudrade.todo.exception.EntityAlreadyExistsException;
import me.rudrade.todo.exception.InvalidAccessException;
import me.rudrade.todo.exception.InvalidDataException;
import me.rudrade.todo.exception.UnexpectedErrorException;
import me.rudrade.todo.model.PasswordRequest;
import me.rudrade.todo.model.User;
import me.rudrade.todo.model.UserRequest;
import me.rudrade.todo.model.types.Role;
import me.rudrade.todo.repository.PasswordRequestRepository;
import me.rudrade.todo.repository.UserRepository;
import me.rudrade.todo.repository.UserRequestRepository;
import me.rudrade.todo.util.ServiceUtil;

@Service
@Validated
@AllArgsConstructor
public class UserService extends ServiceUtil {

    private final UserRepository userRepository;
    private final UserRequestRepository userRequestRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final PasswordRequestRepository passwordRequestRepository;

    public UserRequest createUser(@NotNull UserRequestDto request) {
        var user = Mapper.toUserRequest(request);
        user.setDtCreated(LocalDateTime.now());
        validate(user);

        validateAlreadyExists(user.getUsername(), user.getEmail(), null);

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        userRequestRepository.save(user);

        mailService.sendActivationMail(user);

        return user;
    }

    public User getById(
        @NotNull(message = "User id must be provided.") UUID id,
        @NotNull User requester
    ) {
        if (id.equals(requester.getId()))
            return requester;

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
        if (data.getUsername() == null &&
            data.getPassword() == null &&
            data.getEmail() == null &&
            data.getRole() == null &&
            data.getActive() == null) {
            throw new InvalidDataException("A property must be set to update the resource.");
        }

        // Validate access
        var user = validateUpdateAccess(id, data, requester);

        // If changing username or email, validate if already doesn't exist on in DB
        validateAlreadyExists(data.getUsername(), data.getEmail(), id);

        // Update only setted values
        if (data.getUsername() != null) {
            user.setUsername(data.getUsername());
        }

        if (data.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(data.getPassword()));
        }

        if (data.getEmail() != null) {
            user.setEmail(data.getEmail());
        }

        if (data.getRole() != null) {
            user.setRole(data.getRole());
        }

        if (data.getActive() != null) {
            user.setActive(data.getActive());
        }

        // Save resource
        return userRepository.save(user);
    }

    private User validateUpdateAccess(@NotNull UUID id, @NotNull UserChangeDto data, @NotNull User requester) {
        // Only admins can update role or active properties
        if (!Role.ROLE_ADMIN.equals(requester.getRole()) && (data.getRole() != null || data.getActive() != null))
            throw new InvalidAccessException();

        // Validate if resource exists
        User user = userRepository.findById(id)
            .orElseThrow(() -> new InvalidDataException("User not found."));

        // Validate if is either is admin or is own-user
        if (!Role.ROLE_ADMIN.equals(requester.getRole()) && !id.equals(requester.getId())) {
            throw new InvalidAccessException();
        }

        // If is own user, validate password
        if (id.equals(requester.getId())) {
            var matches = false;
            if (data.getOldPassword() != null) {
                matches =  passwordEncoder.matches(data.getOldPassword(), user.getPassword());
            }

            if (!matches) {
                throw new InvalidDataException("Old password is invalid");
            }
        }

        // Admin - Only own user can update password
        if (Role.ROLE_ADMIN.equals(requester.getRole()) && !id.equals(requester.getId()) && data.getPassword() != null)  {
            throw new InvalidAccessException();
        }

        return user;
    }

    private void validateAlreadyExists(String username, String email, UUID id) {
        var existsUser = userRepository.findActiveByUsernameOrEmail(username, email);
        if (!existsUser.isEmpty()) {
            var allIdMatches = false;
            if (id != null) {
                allIdMatches = existsUser.stream()
                    .allMatch(user -> user.getId().equals(id));
            }
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

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public List<RequestDto> findAllRequest(String filterType, String filterValue) {
        Iterable<UserRequest> iterable;
        if ("EMAIL".equals(filterType)) {
            iterable = userRequestRepository.findAllByEmailContainingIgnoringCase(filterValue);

        } else if ("USERNAME".equals(filterType)) {
            iterable = userRequestRepository.findAllByUsernameContainingIgnoringCase(filterValue);

        } else {
            iterable = userRequestRepository.findAll();
        }

        List<RequestDto> lst = new ArrayList<>();
        iterable.forEach(request -> lst.add(Mapper.toRequestDto(request)));
        return lst;
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public void resendMail(@NotNull UUID id) {
        var request = userRequestRepository.findById(id);
        if (request.isEmpty())
            throw new InvalidDataException("User request not found");

        boolean result = mailService.sendActivationMail(request.get());
        if (!result) {
            throw new UnexpectedErrorException("An error occured while trying to resend mail.");
        }
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public void deleteUserRequest(@NotNull UUID id) {
        userRequestRepository.deleteById(id);
    }

    public void resetPassword(@NotNull PasswordResetDto body) {
        // Validate if one of the values is passed
        if (body.getUsername() == null && body.getEmail() == null)
            throw new InvalidDataException("A valid username or email must be provided");

        // Find user by either username or email
        var optUser = userRepository.findActiveByUsernameOrEmail(body.getUsername(), body.getEmail());
        if (!optUser.isEmpty()) {
            // If found, register request and send email
            var request = new PasswordRequest();
            request.setUser(optUser.getFirst());
            request.setDtCreated(LocalDateTime.now());
            passwordRequestRepository.save(request);

            mailService.sendPasswordReset(request);
        }

    }

    public void setNewPassword(@NotNull UUID id, @NotNull NewPasswordDto body) {
        // Validate body
        if (body.getPassword() == null || body.getPassword().isBlank()) {
            throw new InvalidDataException("Password must be filled.");
        }

        // Find the request
        var request = passwordRequestRepository.findById(id).orElseThrow(() -> new InvalidDataException("Password request not found."));

        // Set new password
        var user = request.getUser();
        user.setPassword(passwordEncoder.encode(body.getPassword()));
        userRepository.save(user);

        // Delete the pending request
        passwordRequestRepository.deleteById(id);
    }
}
