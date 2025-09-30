package me.rudrade.todo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/todo/api")
public class TodoController {

	@GetMapping("/hello")
	public String helloWorld() {
		return "Hello World !";
	}
	
}
