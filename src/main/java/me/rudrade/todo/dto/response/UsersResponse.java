package me.rudrade.todo.dto.response;

import java.util.List;

import me.rudrade.todo.dto.UserDto;

public record UsersResponse(List<UserDto> users) {
    
}
