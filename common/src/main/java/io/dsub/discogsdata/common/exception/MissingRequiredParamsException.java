package io.dsub.discogsdata.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class MissingRequiredParamsException extends BaseException {

    public MissingRequiredParamsException(String reason) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, reason);
    }
    public MissingRequiredParamsException(HttpStatus httpStatus) {
        super(httpStatus, "Missing required request parameter");
    }

    public MissingRequiredParamsException(HttpStatus httpStatus, String reason) {
        super(httpStatus, reason);
    }
}