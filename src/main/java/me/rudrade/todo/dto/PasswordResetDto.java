package me.rudrade.todo.dto;

import static me.rudrade.todo.util.ServiceUtil.trimString;

import lombok.Getter;

@Getter
public class PasswordResetDto {

    private final String username;
    private final String email;

    public PasswordResetDto(String username, String email) {
        this.username = trimString(username);
        this.email = trimString(email);
    }

}
