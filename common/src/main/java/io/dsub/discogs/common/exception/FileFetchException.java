package io.dsub.discogs.common.exception;

import org.springframework.http.HttpStatus;

public class FileFetchException extends BaseException {

  public FileFetchException(String reason) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, reason);
  }
}
