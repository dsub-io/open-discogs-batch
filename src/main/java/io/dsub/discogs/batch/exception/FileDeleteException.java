package io.dsub.discogs.batch.exception;

public class FileDeleteException extends FileException {
  public FileDeleteException(String message) {
    super(message);
  }
  public FileDeleteException(String message, Throwable cause) {
    super(message, cause);
  }
}