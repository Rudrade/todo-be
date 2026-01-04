package me.rudrade.todo.exception;

public class UnexpectedErrorException extends RuntimeException {

    public UnexpectedErrorException(String message) {
        super(message);
    }

}
