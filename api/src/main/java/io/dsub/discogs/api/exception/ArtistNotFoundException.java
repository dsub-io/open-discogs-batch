package io.dsub.discogs.api.exception;

import io.dsub.discogs.common.exception.BaseException;
import org.springframework.http.HttpStatus;

public class ArtistNotFoundException extends BaseException {

  public ArtistNotFoundException(String statusText) {
    super(HttpStatus.NOT_FOUND, statusText);
  }
}
