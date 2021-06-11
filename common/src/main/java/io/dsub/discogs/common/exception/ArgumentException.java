package io.dsub.discogs.common.exception;

import org.springframework.http.HttpStatus;

public class ArgumentException extends BaseException {

  public ArgumentException(String reason) {
    this(HttpStatus.BAD_REQUEST, reason);
  }

  public ArgumentException(HttpStatus httpStatus, String reason) {
    super(httpStatus, reason);
  }
}
