package me.rudrade.todo.dto.response;

import me.rudrade.todo.dto.TaskDto;

import java.util.List;

public record TaskListResponse(long count, List<TaskDto> tasks) {
}
