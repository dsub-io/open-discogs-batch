package io.dsub.discogs.batch.exception;

public class DumpNotFoundException extends BaseRuntimeException {
  public DumpNotFoundException(String message) {
    super(message);
  }
}
