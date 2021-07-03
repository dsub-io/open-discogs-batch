package io.dsub.discogs.batch.exception;

public class BaseCheckedException extends Exception {
    public BaseCheckedException(String message) {
        super(message);
    }

    public BaseCheckedException(String message, Throwable cause) {
        super(message, cause);
    }
}