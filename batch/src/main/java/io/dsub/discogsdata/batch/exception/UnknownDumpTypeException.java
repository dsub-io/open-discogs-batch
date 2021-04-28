package io.dsub.discogsdata.batch.exception;

import io.dsub.discogsdata.common.exception.BaseException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Unknown dump type")
public class UnknownDumpTypeException extends BaseException {
    public UnknownDumpTypeException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}