package io.dsub.discogs.batch.exception;

public class MissingRequiredParamsException extends BaseRuntimeException {
    public MissingRequiredParamsException(String message) {
        super(message);
    }
}
