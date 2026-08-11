package io.dsub.discogs.batch.exception;

public class InitializationFailureException extends BaseRuntimeException {

  public InitializationFailureException(String message) {
    super(message);
  }

  public InitializationFailureException(String message, Throwable cause) {
    super(message, cause);
  }
}
