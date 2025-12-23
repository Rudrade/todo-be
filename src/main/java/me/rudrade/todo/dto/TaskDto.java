package me.rudrade.todo.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TaskDto(UUID id, String title, String description, LocalDate dueDate, String listName, List<TagDto> tags) {

}
