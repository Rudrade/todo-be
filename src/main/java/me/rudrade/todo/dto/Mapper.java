package me.rudrade.todo.dto;

import me.rudrade.todo.model.Tag;
import me.rudrade.todo.model.UserList;
import me.rudrade.todo.model.UserRequest;
import me.rudrade.todo.model.Task;
import me.rudrade.todo.model.User;

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
				dto.getId(),
				dto.getTitle(),
				dto.getDescription(),
				dto.getDueDate(),
				null,
				dto.getListName() == null ? null : new UserList(null, dto.getListName(), null, null, null),
				dto.getTags() != null && !dto.getTags().isEmpty() ? dto.getTags().stream().map(Mapper::toTag).toList() : null
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

	public static UserRequest toUserRequest(UserRequestDto dto) {
		var user = new UserRequest();
		user.setUsername(dto.getUsername());
		user.setPassword(dto.getPassword());
		user.setEmail(dto.getEmail());
		user.setRole(dto.getRole());
		return user;
	}

	public static UserDto toUserDto(User user) {
		return new UserDto(
			user.getId(),
			user.getUsername(),
			user.getEmail(),
			user.getRole(),
			user.isActive()
		);
	}

	public static RequestDto toRequestDto(UserRequest request) {
		return new RequestDto(request.getId(), request.getUsername(), request.getEmail(), request.getDtCreated(), request.isMailSent(), request.getRole());
	}

}
