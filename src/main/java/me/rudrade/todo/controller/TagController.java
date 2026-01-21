package me.rudrade.todo.controller;

import me.rudrade.todo.dto.Mapper;
import me.rudrade.todo.dto.TagDto;
import me.rudrade.todo.dto.response.TagListResponse;
import me.rudrade.todo.model.User;
import me.rudrade.todo.service.AuthenticationService;
import me.rudrade.todo.service.TagService;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tag")
public class TagController {

    private final AuthenticationService authenticationService;
    private final TagService tagService;

    public TagController(AuthenticationService authenticationService, TagService tagService) {
        this.authenticationService = authenticationService;
        this.tagService = tagService;
    }

    @GetMapping()
    public TagListResponse getAll(@RequestHeader(HttpHeaders.AUTHORIZATION) String authToken) {
        User user = authenticationService.getUserByAuth(authToken);

        List<TagDto> lstDto = tagService.findByUser(user).stream()
            .map(Mapper::toTagDto)
            .toList();

        return new TagListResponse(lstDto);
    }

    @DeleteMapping("/{id}")
    public void deleteTag(@RequestHeader(HttpHeaders.AUTHORIZATION) String authToken, @PathVariable UUID id) {
        tagService.deleteById(id, authenticationService.getUserByAuth(authToken));
    }
}
