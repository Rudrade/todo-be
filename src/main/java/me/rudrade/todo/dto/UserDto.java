package me.rudrade.todo.dto;

import java.util.UUID;

import lombok.Data;
import me.rudrade.todo.model.types.Role;

@Data
public class UserDto {
    private final UUID id;
    private final String username;
    private final String email;
    private final Role role;
    private final boolean active;
    private String imageUrl;
}

