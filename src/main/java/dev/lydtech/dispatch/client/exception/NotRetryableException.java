package dev.lydtech.dispatch.client.exception;

public class NotRetryableException extends RuntimeException {
    public NotRetryableException(String message) {
        super(message);
    }

    public NotRetryableException(Exception exception) {
        super(exception);
    }
}
