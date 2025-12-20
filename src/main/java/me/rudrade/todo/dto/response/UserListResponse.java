package me.rudrade.todo.dto.response;

import me.rudrade.todo.dto.UserListDto;

import java.util.List;

public record UserListResponse(List<UserListDto> lists) {
}
