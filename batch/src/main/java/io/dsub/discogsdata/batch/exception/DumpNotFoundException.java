package io.dsub.discogsdata.batch.exception;

import io.dsub.discogsdata.common.exception.BaseException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND)
public class DumpNotFoundException extends BaseException {
    public DumpNotFoundException(HttpStatus httpStatus, String reason) {
        super(httpStatus, reason);
    }

    public DumpNotFoundException(String etag) {
        super(HttpStatus.NOT_FOUND, "dump with etag " + etag + " not found");
    }
}
