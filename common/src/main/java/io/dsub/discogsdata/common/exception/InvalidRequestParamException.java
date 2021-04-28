package io.dsub.discogsdata.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class InvalidRequestParamException extends BaseException {
    public InvalidRequestParamException(String reason) {
        super(HttpStatus.BAD_REQUEST, reason);
    }
}
