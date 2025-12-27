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
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/todo/api/tag")
public class TagController {

    private final AuthenticationService authenticationService;
    private final TagService tagService;

    public TagController(AuthenticationService authenticationService, TagService tagService) {
        this.authenticationService = authenticationService;
        this.tagService = tagService;
    }

    @GetMapping()
    public TagListResponse getAll(@RequestHeader(HttpHeaders.AUTHORIZATION) String authToken) {
        Optional<User> user = authenticationService.getUserByAuth(authToken);
        if (user.isEmpty())
            return new TagListResponse(List.of());

        List<TagDto> lstDto = tagService.findByUser(user.get()).stream()
            .map(Mapper::toTagDto)
            .toList();

        return new TagListResponse(lstDto);
    }

    @DeleteMapping("/{id}")
    public void deleteTag(@PathVariable UUID id) {
        tagService.deleteById(id);
    }
}
