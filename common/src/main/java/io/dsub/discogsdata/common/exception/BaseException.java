package io.dsub.discogsdata.common.exception;

import org.springframework.http.HttpStatus;

import java.util.function.Supplier;

public abstract class BaseException extends RuntimeException {
  private final HttpStatus httpStatus;

  public Supplier<BaseException> toSupplier() {
    return () -> this;
  }

  public BaseException(HttpStatus httpStatus, String reason) {
    super(reason);
    this.httpStatus = httpStatus;
  }

  public HttpStatus getHttpStatus() {
    return this.httpStatus;
  }
}
