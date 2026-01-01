package me.rudrade.todo.dto;

import me.rudrade.todo.model.types.Role;

public record UserRequestDto(String username, String password, String email, Role role) {

}
