package io.dsub.discogs.batch.exception;

public class MissingRequiredArgumentException extends BaseRuntimeException {
  public MissingRequiredArgumentException(String message) {
    super(message);
  }
}
