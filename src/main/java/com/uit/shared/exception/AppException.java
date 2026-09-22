package com.uit.shared.exception;

/**
 * Failure that the application can report. Features throw this instead of a toolkit exception.
 */
public class AppException extends RuntimeException {

    public AppException(String message) {
        super(message);
    }

    public AppException(String message, Throwable cause) {
        super(message, cause);
    }
}
