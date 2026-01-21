package me.rudrade.todo.dto;

import lombok.Getter;
import me.rudrade.todo.model.types.Language;
import me.rudrade.todo.model.types.Role;

import static me.rudrade.todo.util.ServiceUtil.trimString;

@Getter
public class UserRequestDto {

    private final String username;
    private final String password;
    private final String email;
    private final Role role;
    private final Language language;

    public UserRequestDto(String username, String password, String email, Role role, Language language) {
        this.username = trimString(username);
        this.password = trimString(password);
        this.email = trimString(email);
        this.role = role;
        this.language = language;
    }

}
