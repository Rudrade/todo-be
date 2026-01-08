package me.rudrade.todo.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import me.rudrade.todo.model.PasswordRequest;

@Repository
public interface PasswordRequestRepository extends CrudRepository<PasswordRequest, UUID> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @NativeQuery("delete from password_request pr where timestampdiff(minute, pr.dt_created, current_timestamp) >= ?1")
    void deleteIfExpired(int minutes);

    List<PasswordRequest> findAllByMailSentIsFalse();

}
