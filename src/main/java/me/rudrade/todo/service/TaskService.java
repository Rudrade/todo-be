package me.rudrade.todo.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import jakarta.annotation.Nonnull;
import me.rudrade.todo.dto.Mapper;
import me.rudrade.todo.dto.TaskDto;
import me.rudrade.todo.exception.TaskNotFoundException;
import me.rudrade.todo.model.Task;
import me.rudrade.todo.repository.TaskRepository;

@Service
public class TaskService {

	private final TaskRepository repository;
	private final Mapper mapper;

    public TaskService(TaskRepository repository, Mapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }
	
	public TaskDto saveTask(@Nonnull TaskDto input) {

		if (input.id() != null) {
			Optional<Task> optTask = repository.findById(input.id());
			if (optTask.isEmpty()) {
				throw new TaskNotFoundException();
			}
		}
		
		Task task = repository.save(mapper.toTask(input));
		return mapper.toTaskDto(task);
	}
	
	public List<TaskDto> getAll() {
		List<TaskDto> lst = new ArrayList<>();
		
		repository.findAll()
			.forEach(t -> lst.add(mapper.toTaskDto(t)));
		
		return lst;
	}
	
	public TaskDto getById(@Nonnull UUID id) {
		Optional<Task> optTask = repository.findById(id);
		if (optTask.isEmpty()) {
			throw new TaskNotFoundException();
		}
		
		return mapper.toTaskDto(optTask.get());
	}
	
	public void deleteById(@Nonnull UUID id) {
		Optional<Task> optTask = repository.findById(id);
		if (optTask.isEmpty()) {
			throw new TaskNotFoundException();
		}
		
		repository.deleteById(id);
	}
	
}
