package me.rudrade.todo.repository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import me.rudrade.todo.model.User;

@Repository
public interface UserRepository extends CrudRepository<User, UUID> {
	
	Optional<User> findByUsername(String username);

	List<User> findActiveByUsernameOrEmail(String username, String email);

	List<User> findByActive(boolean active);

	List<User> findByUsernameContainingIgnoreCase(String username);

	List<User> findByEmailContainingIgnoreCase(String email);

	List<User> findByActiveAndUsernameContainingIgnoreCase(boolean active, String username);

	List<User> findByActiveAndEmailContainingIgnoreCase(boolean active, String email);

}
