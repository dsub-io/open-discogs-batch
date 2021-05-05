package io.dsub.discogsdata.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class ClassNotRegisteredException extends BaseException {
  public ClassNotRegisteredException(String reason) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, reason);
  }
}
