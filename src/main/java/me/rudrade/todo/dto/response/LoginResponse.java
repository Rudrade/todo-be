package me.rudrade.todo.dto.response;

import me.rudrade.todo.model.types.Language;

public record LoginResponse(String token, String imageUrl, Language language) {

}
