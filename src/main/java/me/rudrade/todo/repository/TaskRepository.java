package me.rudrade.todo.repository;

import java.util.UUID;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import me.rudrade.todo.model.Task;

@Repository
public interface TaskRepository extends CrudRepository<Task, UUID>{

}
