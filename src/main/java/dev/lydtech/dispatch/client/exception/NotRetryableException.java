package dev.lydtech.dispatch.client.exception;

public class NotRetryableException extends RuntimeException {

    /** In some cases there was a constructor used only
     * with String message, in which case it could lead to inconsistencies
     * The removal of super(message) forced the calling code to always pass the original
     * exception, which could have ensured correct error propagation. **/
    public NotRetryableException(Exception exception) {
        super(exception);
    }
}
