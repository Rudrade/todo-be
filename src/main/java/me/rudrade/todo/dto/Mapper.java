package me.rudrade.todo.dto;

import org.springframework.stereotype.Component;

import me.rudrade.todo.model.Task;

@Component
public class Mapper {

	public TaskDto toTaskDto(Task task) {
		return new TaskDto(
				task.getId(),
				task.getTitle(),
				task.getDescription(),
				task.getDueDate()
				);
	}
	
	public Task toTask(TaskDto dto) {
		return new Task(
				dto.id(),
				dto.title(),
				dto.description(),
				dto.dueDate()
				);
	}
	
}
