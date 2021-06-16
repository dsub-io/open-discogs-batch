package io.dsub.discogs.batch.exception;

public class InvalidArgumentException extends BaseRuntimeException {
  public InvalidArgumentException(String message) {
    super(message);
  }
}