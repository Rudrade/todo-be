package me.rudrade.todo.dto;

import me.rudrade.todo.model.Tag;
import me.rudrade.todo.model.UserList;

import me.rudrade.todo.model.Task;

public class Mapper {

	private Mapper() {}

	public static TaskDto toTaskDto(Task task) {
		return new TaskDto(
				task.getId(),
				task.getTitle(),
				task.getDescription(),
				task.getDueDate(),
				task.getUserList() == null ? null : task.getUserList().getName(),
				task.getTags() != null && !task.getTags().isEmpty() ? task.getTags().stream().map(Mapper::toTagDto).toList() : null
				);
	}
	
	public static Task toTask(TaskDto dto) {
		return new Task(
				dto.id(),
				dto.title(),
				dto.description(),
				dto.dueDate(),
				null,
				dto.listName() == null ? null : new UserList(null, dto.listName(), null, null, null),
				dto.tags() != null && !dto.tags().isEmpty() ? dto.tags().stream().map(Mapper::toTag).toList() : null
				);
	}

	public static TagDto toTagDto(Tag tag) {
		return new TagDto(tag.getName(), tag.getColor());
	}

	public static Tag toTag(TagDto dto) {
		return new Tag(null, dto.name(), dto.color(), null, null);
	}

	public static UserListDto toListDto(UserList lst) {
		return new UserListDto(lst.getName(), lst.getColor(), lst.getTasks() == null ? 0 : lst.getTasks().size());
	}
}
