package io.dsub.discogs.batch.exception;

public class UnsupportedDatabaseException extends BaseCheckedException {
    public UnsupportedDatabaseException(String message) {
        super(message);
    }

    public UnsupportedDatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
