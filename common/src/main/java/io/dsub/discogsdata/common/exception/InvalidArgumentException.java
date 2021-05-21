package io.dsub.discogsdata.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidArgumentException extends BaseException {

  public InvalidArgumentException(String reason) {
    super(HttpStatus.BAD_REQUEST, reason);
  }
}
