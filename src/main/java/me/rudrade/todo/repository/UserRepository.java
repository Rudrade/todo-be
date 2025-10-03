package me.rudrade.todo.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import me.rudrade.todo.model.User;

@Repository
public interface UserRepository extends CrudRepository<User, UUID> {
	
	Optional<User> findByUsername(String username);

}
