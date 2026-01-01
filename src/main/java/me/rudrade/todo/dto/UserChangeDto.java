package me.rudrade.todo.dto;

import me.rudrade.todo.model.types.Role;

public record UserChangeDto(String username, String password, String email, Role role, Boolean active) {

}
