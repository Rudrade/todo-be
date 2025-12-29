package me.rudrade.todo.controller;

import java.util.UUID;

import me.rudrade.todo.dto.filter.TaskListFilter;
import me.rudrade.todo.dto.response.TaskListResponse;
import me.rudrade.todo.dto.response.UserListResponse;
import me.rudrade.todo.service.AuthenticationService;
import me.rudrade.todo.service.UserListService;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

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
	public TaskDto saveTask(@RequestBody TaskDto task,
							@RequestHeader(HttpHeaders.AUTHORIZATION) String authToken) {

		return service.saveTask(task, authenticationService.getUserByAuth(authToken));
	}
	
	@GetMapping()
	public TaskListResponse getAll(
		@RequestHeader(HttpHeaders.AUTHORIZATION) String authToken,
		@Param("filter") String filter, @Param("searchTerm") String searchTerm) {

        TaskListFilter listFilter = new TaskListFilter(
                filter==null || filter.isEmpty() ? null : TaskListFilter.Filter.valueOf(filter.toUpperCase()),
                searchTerm,
				authenticationService.getUserByAuth(authToken));

        return service.getAll(listFilter);
	}
	
	@GetMapping("/detail/{id}")
	public TaskDto getDetail(
		@RequestHeader(HttpHeaders.AUTHORIZATION) String authToken,
		@PathVariable UUID id) {

		return service.getById(id, authenticationService.getUserByAuth(authToken));
	}
	
	@DeleteMapping("/remove/{id}")
	public void delete(
		@RequestHeader(HttpHeaders.AUTHORIZATION) String authToken,
		@PathVariable UUID id) {

		service.deleteById(id, authenticationService.getUserByAuth(authToken));
	}

	@GetMapping("/lists")
	public UserListResponse getUserLists(@RequestHeader(HttpHeaders.AUTHORIZATION) String authToken) {
		return new UserListResponse(userListService.getUserListsByToken(authToken));
	}
}
