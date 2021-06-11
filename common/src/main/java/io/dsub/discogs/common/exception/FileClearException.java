package io.dsub.discogs.common.exception;

import org.springframework.http.HttpStatus;

public class FileClearException extends BaseException {

  public FileClearException(String reason) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, reason);
  }
}
