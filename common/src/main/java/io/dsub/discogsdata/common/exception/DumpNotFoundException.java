package io.dsub.discogsdata.common.exception;

import org.springframework.http.HttpStatus;

public class DumpNotFoundException extends BaseException {

  public DumpNotFoundException(String reason) {
    super(HttpStatus.NOT_FOUND, reason);
  }

  public DumpNotFoundException(HttpStatus httpStatus, String reason) {
    super(httpStatus, reason);
  }
}
