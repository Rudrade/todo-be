package me.rudrade.todo.dto.response;

import java.util.List;

import me.rudrade.todo.dto.RequestDto;

public record RequestListResponse(List<RequestDto> requests) {

}
