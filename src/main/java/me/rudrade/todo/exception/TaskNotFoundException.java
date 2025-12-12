package me.rudrade.todo.exception;

public class TaskNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 5337346326867790410L;
	
	public TaskNotFoundException() {
		super("The task with the given id doesn't exist.");
	}

}
