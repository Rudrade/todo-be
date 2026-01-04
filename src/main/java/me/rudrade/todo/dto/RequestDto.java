package me.rudrade.todo.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import me.rudrade.todo.model.types.Role;

public record RequestDto(UUID id, String username, String email, LocalDateTime dtCreated, boolean mailSent, Role role) {

}
