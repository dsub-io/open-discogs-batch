package io.dsub.discogsdata.common.exception;

import org.springframework.http.HttpStatus;

public class UnknownColumnException extends BaseException {

  public UnknownColumnException(String reason) {
    super(HttpStatus.BAD_REQUEST, reason);
  }

  public UnknownColumnException(HttpStatus httpStatus, String reason) {
    super(httpStatus, reason);
  }
}
