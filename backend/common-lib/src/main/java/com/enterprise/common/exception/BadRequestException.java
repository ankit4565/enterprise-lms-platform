package com.enterprise.common.exception;

public class BadRequestException extends ApiException {

    public BadRequestException(ErrorCode errorCode) {
        super(errorCode);
    }

    public BadRequestException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public BadRequestException(String message) {
        super(ErrorCode.INVALID_REQUEST, message);
    }
}
