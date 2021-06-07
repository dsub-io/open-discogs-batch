package io.dsub.discogs.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class InitializationFailureException extends BaseException {

  public InitializationFailureException(String reason) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, reason);
  }
}
