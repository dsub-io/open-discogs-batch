package io.dsub.discogsdata.common.exception;

import org.springframework.http.HttpStatus;

public class MalformedArgumentException extends ArgumentException {
  public MalformedArgumentException(String reason) {
    super(reason);
  }

  public MalformedArgumentException(HttpStatus httpStatus, String reason) {
    super(httpStatus, reason);
  }
}
