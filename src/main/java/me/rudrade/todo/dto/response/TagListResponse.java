package me.rudrade.todo.dto.response;

import me.rudrade.todo.dto.TagDto;

import java.util.List;

public record TagListResponse(List<TagDto> tags) {
}
