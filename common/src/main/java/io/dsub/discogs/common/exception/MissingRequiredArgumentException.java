package io.dsub.discogs.common.exception;

import org.springframework.http.HttpStatus;

public class MissingRequiredArgumentException extends ArgumentException {

  public MissingRequiredArgumentException(String reason) {
    super(reason);
  }

  public MissingRequiredArgumentException(HttpStatus httpStatus, String reason) {
    super(httpStatus, reason);
  }
}
