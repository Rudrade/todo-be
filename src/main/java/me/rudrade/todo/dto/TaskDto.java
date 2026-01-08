package me.rudrade.todo.dto;

import static me.rudrade.todo.util.ServiceUtil.trimString;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class TaskDto {
    private final UUID id;
    private final String title;
    private final String description;
    private final LocalDate dueDate;
    private final String listName;
    private final List<TagDto> tags;

    public TaskDto(UUID id, String title, String description, LocalDate dueDate, String listName, List<TagDto> tags) {
        this.id = id;
        this.title = trimString(title);
        this.description = trimString(description);
        this.dueDate = dueDate;
        this.listName = trimString(listName);
        this.tags = tags;
    }

}
