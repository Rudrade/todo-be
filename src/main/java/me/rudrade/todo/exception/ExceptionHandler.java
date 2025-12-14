package me.rudrade.todo.exception;

import org.slf4j.Logger;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice
public class ExceptionHandler {

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ExceptionHandler.class);

	@org.springframework.web.bind.annotation.ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<Error> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
		return new ResponseEntity<>(new Error("Invalid data sent"), HttpStatus.BAD_REQUEST);
	}
	
	@org.springframework.web.bind.annotation.ExceptionHandler(TaskNotFoundException.class)
	public ResponseEntity<Error> handleTaskNotFoundException(TaskNotFoundException ex) {
		return new ResponseEntity<>(new Error(ex.getMessage()), HttpStatus.NOT_FOUND);
	}

    @org.springframework.web.bind.annotation.ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Error> handleRuntimeException(RuntimeException ex) {
        LOGGER.error("Uncaught exception", ex);
        return new ResponseEntity<>(new Error("Internal server error"), HttpStatus.INTERNAL_SERVER_ERROR);
    }

	
    public record Error(String message) {}
}
