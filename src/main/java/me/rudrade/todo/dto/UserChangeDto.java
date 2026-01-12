package me.rudrade.todo.dto;

import lombok.Getter;
import me.rudrade.todo.model.types.Language;
import me.rudrade.todo.model.types.Role;

import static me.rudrade.todo.util.ServiceUtil.trimString;

import org.springframework.web.multipart.MultipartFile;

@Getter
public class UserChangeDto {

    private final String username;
    private final String password;
    private final String email;
    private final Role role;
    private final Boolean active;
    private final String oldPassword;
    private final MultipartFile image;
    private final Language language;

    public UserChangeDto(String username, String password, String email, Role  role, Boolean active, String oldPassword, MultipartFile image, Language language) {
        this.username = trimString(username);
        this.password = trimString(password);
        this.email = trimString(email);
        this.role = role;
        this.active = active;
        this.oldPassword = trimString(oldPassword);
        this.image = image;
        this.language = language;
    }
}
