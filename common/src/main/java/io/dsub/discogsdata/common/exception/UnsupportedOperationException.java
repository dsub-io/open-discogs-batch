package io.dsub.discogsdata.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class UnsupportedOperationException extends BaseException {

  public UnsupportedOperationException(String reason) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, reason);
  }

  public UnsupportedOperationException(HttpStatus httpStatus, String reason) {
    super(httpStatus, reason);
  }
}
