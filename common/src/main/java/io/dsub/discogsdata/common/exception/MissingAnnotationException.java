package io.dsub.discogsdata.common.exception;

import org.springframework.http.HttpStatus;

public class MissingAnnotationException extends BaseException {

  public MissingAnnotationException(String reason) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, reason);
  }

  public MissingAnnotationException(Class<?> targetClass, Class<?> annotation) {
    super(
        HttpStatus.INTERNAL_SERVER_ERROR,
        targetClass.getName() + " expected to be annotated with @" + annotation.getSimpleName());
  }
}
