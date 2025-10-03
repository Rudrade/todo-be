package me.rudrade.todo.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice
public class ExceptionHandler {

	@org.springframework.web.bind.annotation.ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<Error> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
		return new ResponseEntity<>(new Error("Invalid data sent"), HttpStatus.BAD_REQUEST);
	}
	
	@org.springframework.web.bind.annotation.ExceptionHandler(TaskNotFoundException.class)
	public ResponseEntity<Error> handleTaskNotFoundException(TaskNotFoundException ex) {
		return new ResponseEntity<>(new Error(ex.getMessage()), HttpStatus.NOT_FOUND);
	}

	
	private record Error(String message) {}
}
