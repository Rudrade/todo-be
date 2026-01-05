package me.rudrade.todo.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import me.rudrade.todo.model.UserRequest;

@Repository
public interface UserRequestRepository extends CrudRepository<UserRequest, UUID> {

    boolean existsByUsernameOrEmail(String username, String email);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @NativeQuery("delete from user_request ur where timestampdiff(minute, ur.dt_created, current_timestamp) >= ?1")
    void deleteIfExpired(int minutes);

    List<UserRequest> findAllByMailSentIsFalse();

    List<UserRequest> findAllByUsernameContainingIgnoringCase(String username);

    List<UserRequest> findAllByEmailContainingIgnoringCase(String email);
}
