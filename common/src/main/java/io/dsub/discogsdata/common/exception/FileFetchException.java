package io.dsub.discogsdata.common.exception;

import io.dsub.discogsdata.common.exception.BaseException;
import org.springframework.http.HttpStatus;

public class FileFetchException extends BaseException {

  public FileFetchException(String reason) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, reason);
  }
}
