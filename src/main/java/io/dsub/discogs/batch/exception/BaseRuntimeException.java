package io.dsub.discogs.batch.exception;

public abstract class BaseRuntimeException extends RuntimeException {
  public BaseRuntimeException(String message) {
    super(message);
  }
}
