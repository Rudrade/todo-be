package me.rudrade.todo.dto;

import me.rudrade.todo.model.UserList;
import org.springframework.stereotype.Component;

import me.rudrade.todo.model.Task;

public class Mapper {

	private Mapper() {}

	public static TaskDto toTaskDto(Task task) {
		return new TaskDto(
				task.getId(),
				task.getTitle(),
				task.getDescription(),
				task.getDueDate(),
				task.getUserList() == null ? null : task.getUserList().getName()
				);
	}
	
	public static Task toTask(TaskDto dto) {
		return new Task(
				dto.id(),
				dto.title(),
				dto.description(),
				dto.dueDate(),
				dto.listName() == null ? null : new UserList(null, dto.listName(), null, null, null)
				);
	}
	
}
