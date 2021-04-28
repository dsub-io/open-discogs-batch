package io.dsub.discogsdata.exception;

import io.dsub.discogsdata.common.exception.BaseException;
import org.springframework.http.HttpStatus;

public class ArtistNotFoundException extends BaseException {
    public ArtistNotFoundException(String statusText) {
        super(HttpStatus.NOT_FOUND, statusText);
    }
}
