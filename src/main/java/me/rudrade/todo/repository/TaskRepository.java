package me.rudrade.todo.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import me.rudrade.todo.model.Task;

@Repository
public interface TaskRepository extends CrudRepository<Task, UUID>{

    @Query("SELECT t FROM Task t WHERE t.dueDate = CURRENT_DATE")
    List<Task> findDueToday();

    @Query("SELECT COUNT(t.id) FROM Task t WHERE t.dueDate = CURRENT_DATE")
    long countFindDueToday();

    @Query("SELECT t FROM Task t WHERE t.dueDate > CURRENT_DATE")
    List<Task> findDueUpcoming();

    @Query("SELECT COUNT(t.id) FROM Task t WHERE t.dueDate > CURRENT_DATE")
    long countFindDueUpcoming();
}
