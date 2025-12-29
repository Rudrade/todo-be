package me.rudrade.todo.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import me.rudrade.todo.model.Task;

@Repository
public interface TaskRepository extends CrudRepository<Task, UUID>{

    @Query("SELECT t FROM Task t WHERE t.dueDate = CURRENT_DATE AND t.user.id = ?1")
    List<Task> findDueToday(UUID userId);

    @Query("SELECT t FROM Task t WHERE t.dueDate > CURRENT_DATE AND t.user.id = ?1")
    List<Task> findDueUpcoming(UUID userId);

    @Query("SELECT t FROM Task t WHERE LOWER(t.title) LIKE LOWER(CONCAT('%', ?1, '%')) AND t.user.id = ?2")
    List<Task> findByTitleContains(String title, UUID userId);

    Optional<Task> findByIdAndUserId(UUID id, UUID userId);

    List<Task> findAllByUserId(UUID userId);
}
