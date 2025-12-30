package me.rudrade.todo.dto;

import java.util.UUID;

import me.rudrade.todo.model.types.Role;

public record UserDto(UUID id, String username, String email, Role role, boolean active) {
}

