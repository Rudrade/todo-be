package me.rudrade.todo.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Nonnull;
import me.rudrade.todo.dto.TaskDto;
import me.rudrade.todo.service.TaskService;

@RestController
@RequestMapping("/todo/api")
public class TodoController {
	
	private final TaskService service;

    public TodoController(TaskService service) {
        this.service = service;
    }

	@PostMapping("/save")
	public TaskDto saveTask(@RequestBody @Nonnull TaskDto task) {
		return service.saveTask(task);
	}
	
	@GetMapping("/all")
	public List<TaskDto> getAll() {// TODO: Return with counter, e offset/limit
		return service.getAll();
	}
	
	@GetMapping("/detail/{id}")
	public TaskDto getDetail(@PathVariable @Nonnull UUID id) {
		return service.getById(id);
	}
	
	@DeleteMapping("/remove/{id}")
	public void delete(@PathVariable @Nonnull UUID id) {
		service.deleteById(id);
	}
	
}
