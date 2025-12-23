package me.rudrade.todo.controller;

import java.util.UUID;

import me.rudrade.todo.dto.filter.TaskListFilter;
import me.rudrade.todo.dto.response.TaskListResponse;
import me.rudrade.todo.dto.response.UserListResponse;
import me.rudrade.todo.service.AuthenticationService;
import me.rudrade.todo.service.UserListService;
import org.springframework.data.repository.query.Param;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Nonnull;
import me.rudrade.todo.dto.TaskDto;
import me.rudrade.todo.service.TaskService;

@RestController
@RequestMapping("/todo/api/task")
public class TodoController {
	
	private final TaskService service;
	private final UserListService userListService;
	private final AuthenticationService authenticationService;

	public TodoController(TaskService service, UserListService userListService, AuthenticationService authenticationService) {
		this.service = service;
		this.userListService = userListService;
		this.authenticationService = authenticationService;
	}

	@PostMapping("/save")
	public TaskDto saveTask(@RequestBody @Nonnull TaskDto task, @RequestHeader("Authorization") @Nonnull String authToken) {
		return service.saveTask(task, authenticationService.getUserByAuth(authToken).orElse(null));
	}
	
	@GetMapping()
	public TaskListResponse getAll(@Param("filter") String filter, @Param("searchTerm") String searchTerm) {
        TaskListFilter listFilter = new TaskListFilter(
                filter==null || filter.isEmpty() ? null : TaskListFilter.Filter.valueOf(filter.toUpperCase()),
                searchTerm);
        return service.getAll(listFilter);
	}
	
	@GetMapping("/detail/{id}")
	public TaskDto getDetail(@PathVariable @Nonnull UUID id) {
		return service.getById(id);
	}
	
	@DeleteMapping("/remove/{id}")
	public void delete(@PathVariable @Nonnull UUID id) {
		service.deleteById(id);
	}

	@GetMapping("/lists")
	public UserListResponse getUserLists(@RequestHeader("Authorization") @Nonnull String authToken) {
		return new UserListResponse(userListService.getUserListsByToken(authToken));
	}
}
