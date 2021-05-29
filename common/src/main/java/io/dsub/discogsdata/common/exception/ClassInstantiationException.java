package io.dsub.discogsdata.common.exception;

import org.springframework.http.HttpStatus;

public class ClassInstantiationException extends BaseException {

  public ClassInstantiationException(String reason) {
    this(HttpStatus.INTERNAL_SERVER_ERROR, reason);
  }

  public ClassInstantiationException(HttpStatus httpStatus, String reason) {
    super(httpStatus, reason);
  }

  @Override
  public HttpStatus getHttpStatus() {
    return super.getHttpStatus();
  }
}
