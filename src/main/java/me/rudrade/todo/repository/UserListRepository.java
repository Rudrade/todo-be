package me.rudrade.todo.repository;

import me.rudrade.todo.model.UserList;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserListRepository extends CrudRepository<UserList, UUID> {

    Optional<UserList> findByNameAndUserId(String name, UUID userId);

}
