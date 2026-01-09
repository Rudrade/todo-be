package me.rudrade.todo.exception;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.persistence.RollbackException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

@ControllerAdvice
public class ExceptionHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionHandler.class);

	@org.springframework.web.bind.annotation.ExceptionHandler(AuthorizationDeniedException.class)
	public ResponseEntity<Error> handleAuthorizationDeniedException(AuthorizationDeniedException ex) {
		return handleInvalidAccessException(null);
	}

	@org.springframework.web.bind.annotation.ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<Error> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
		return new ResponseEntity<>(new Error("Invalid data sent", null), HttpStatus.BAD_REQUEST);
	}

	@org.springframework.web.bind.annotation.ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<Error> handleConstraintViolationException(ConstraintViolationException ex) {
		Set<String> setErrors = new HashSet<>();
		if (ex.getConstraintViolations() != null) {
			ex.getConstraintViolations().stream()
				.map(ConstraintViolation::getMessage)
				.forEach(setErrors::add);
		}

		return new ResponseEntity<>(new Error("Invalid data sent", setErrors), HttpStatus.BAD_REQUEST);
	}

	@org.springframework.web.bind.annotation.ExceptionHandler(TransactionSystemException.class)
	public ResponseEntity<Error> handleTransactionSystemException(TransactionSystemException ex) {
		if (ex.getCause() instanceof RollbackException ex2) {
			return handleRollbackException(ex2);
		}
		return handleRuntimeException(ex);
	}

	@org.springframework.web.bind.annotation.ExceptionHandler(RollbackException.class)
	public ResponseEntity<Error> handleRollbackException(RollbackException ex) {
		if (ex.getCause() instanceof ConstraintViolationException ex2) {
			return handleConstraintViolationException(ex2);
		}
		return handleRuntimeException(ex);
	}

	@org.springframework.web.bind.annotation.ExceptionHandler(InvalidDataException.class)
	public ResponseEntity<Error> handleInvalidDataException(InvalidDataException ex) {
		return new ResponseEntity<>(new Error(ex.getMessage(), null), HttpStatus.BAD_REQUEST);
	}

	@org.springframework.web.bind.annotation.ExceptionHandler(EntityAlreadyExistsException.class)
	public ResponseEntity<Error> handleEntityAlreadyExistsException(EntityAlreadyExistsException ex) {
		return new ResponseEntity<>(new Error(ex.getMessage(), null), HttpStatus.CONFLICT);
	}
	
	@org.springframework.web.bind.annotation.ExceptionHandler(InvalidAccessException.class)
	public ResponseEntity<Error> handleInvalidAccessException(InvalidAccessException ex) {
		return new ResponseEntity<>(new Error("Access Invalid", null), HttpStatus.FORBIDDEN);
	}

    @org.springframework.web.bind.annotation.ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Error> handleRuntimeException(RuntimeException ex) {
        LOGGER.error("Uncaught exception", ex);
        return new ResponseEntity<>(new Error("Internal server error", null), HttpStatus.INTERNAL_SERVER_ERROR);
    }

	@org.springframework.web.bind.annotation.ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Error> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
		return new ResponseEntity<>(new Error("Invalid argument", Set.of(ex.getName())), HttpStatus.BAD_REQUEST); 
    }

	@org.springframework.web.bind.annotation.ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Error> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException ex) {
		return new ResponseEntity<>(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

	
    private record Error(String message, Set<String> errors) {}
}
