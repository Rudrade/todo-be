package me.rudrade.todo.dto;

import static me.rudrade.todo.util.ServiceUtil.trimString;

import lombok.Getter;

@Getter
public class NewPasswordDto {

    private final String password;

    public NewPasswordDto(String password) {
        this.password = trimString(password);
    }

}
