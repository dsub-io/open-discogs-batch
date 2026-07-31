package io.dsub.discogs.batch.exception;

public class ImportExecutionException extends BaseCheckedException {

  public ImportExecutionException(String message) {
    super(message);
  }

  public ImportExecutionException(String message, Throwable cause) {
    super(message, cause);
  }
}
