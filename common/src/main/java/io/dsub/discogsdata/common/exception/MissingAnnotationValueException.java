package io.dsub.discogsdata.common.exception;

import org.springframework.http.HttpStatus;

public class MissingAnnotationValueException extends BaseException {

  public MissingAnnotationValueException(String reason) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, reason);
  }

  public MissingAnnotationValueException(
      Class<?> targetClass, Class<?> annotation, String missingEntryName) {
    super(
        HttpStatus.INTERNAL_SERVER_ERROR,
        targetClass.getName()
            + " expected to have @"
            + annotation.getSimpleName()
            + " annotation with "
            + missingEntryName
            + " provided");
  }
}
