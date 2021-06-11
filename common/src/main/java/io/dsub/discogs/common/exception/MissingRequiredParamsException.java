package io.dsub.discogs.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class MissingRequiredParamsException extends InvalidArgumentException {

  public MissingRequiredParamsException(String reason) {
    super(reason);
  }
}
