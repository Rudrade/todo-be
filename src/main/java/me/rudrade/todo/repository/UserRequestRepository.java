package me.rudrade.todo.repository;

import java.util.UUID;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import me.rudrade.todo.model.UserRequest;

@Repository
public interface UserRequestRepository extends CrudRepository<UserRequest, UUID> {

    boolean existsByUsernameOrEmail(String username, String email);

}
