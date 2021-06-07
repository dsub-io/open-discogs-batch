package io.dsub.discogs.common.exception;

import java.util.function.Supplier;
import org.springframework.http.HttpStatus;

public abstract class BaseException extends RuntimeException {

  private final HttpStatus httpStatus;

  public BaseException(HttpStatus httpStatus, String reason) {
    super(reason);
    this.httpStatus = httpStatus;
  }

  public Supplier<BaseException> toSupplier() {
    return () -> this;
  }

  public HttpStatus getHttpStatus() {
    return this.httpStatus;
  }
}
