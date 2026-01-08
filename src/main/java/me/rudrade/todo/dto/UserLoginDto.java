package me.rudrade.todo.dto;

import static me.rudrade.todo.util.ServiceUtil.trimString;

import lombok.Getter;

@Getter
public class UserLoginDto {
    private final String username;
    private final String password;

    public UserLoginDto(String username, String password) {
        this.username = trimString(username);
        this.password = trimString(password);
    }

}
