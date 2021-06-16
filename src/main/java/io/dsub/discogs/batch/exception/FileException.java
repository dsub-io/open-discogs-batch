package io.dsub.discogs.batch.exception;

public class FileException extends BaseCheckedException {
  public FileException(String message) {
    super(message);
  }

  public FileException(String message, Throwable cause) {
    super(message, cause);
  }
}
