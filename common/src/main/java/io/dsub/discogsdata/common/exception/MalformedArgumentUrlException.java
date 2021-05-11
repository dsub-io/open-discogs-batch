package io.dsub.discogsdata.common.exception;

import org.springframework.http.HttpStatus;

public class MalformedArgumentUrlException extends ArgumentException {
  public MalformedArgumentUrlException(String reason) {
    super(reason);
  }

  public MalformedArgumentUrlException(HttpStatus httpStatus, String reason) {
    super(httpStatus, reason);
  }
}
