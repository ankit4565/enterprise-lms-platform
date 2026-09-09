package com.enterprise.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    VALIDATION_FAILED("Validation failed", HttpStatus.BAD_REQUEST),
    INVALID_REQUEST("Invalid request parameters", HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND("Requested resource not found", HttpStatus.NOT_FOUND),
    CONFLICT("Resource conflict", HttpStatus.CONFLICT),

    // Auth specific error codes
    USER_ALREADY_EXISTS("User with given email already exists", HttpStatus.CONFLICT),
    USER_NOT_FOUND("User not found", HttpStatus.NOT_FOUND),
    INVALID_CREDENTIALS("Invalid email or password", HttpStatus.UNAUTHORIZED),
    ACCOUNT_NOT_VERIFIED("Account email has not been verified yet", HttpStatus.FORBIDDEN),
    ACCOUNT_LOCKED("Account is temporarily locked due to failed attempts", HttpStatus.LOCKED),
    ACCOUNT_SUSPENDED("Account has been suspended", HttpStatus.FORBIDDEN),

    // Tokens
    TOKEN_INVALID("Invalid or malformed token", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED("Token has expired", HttpStatus.UNAUTHORIZED),
    TOKEN_REUSE_DETECTED("Security alert: Refresh token reuse detected, sessions revoked", HttpStatus.UNAUTHORIZED),

    // OTP
    OTP_INVALID("Invalid OTP code", HttpStatus.BAD_REQUEST),
    OTP_EXPIRED("OTP has expired", HttpStatus.BAD_REQUEST),
    OTP_MAX_ATTEMPTS_EXCEEDED("Maximum OTP verification attempts exceeded", HttpStatus.TOO_MANY_REQUESTS),

    // Security & Authorization
    UNAUTHORIZED("Authentication required", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED("Access denied. Insufficient permissions", HttpStatus.FORBIDDEN),
    RATE_LIMIT_EXCEEDED("Too many requests. Please try again later", HttpStatus.TOO_MANY_REQUESTS),

    // System
    INTERNAL_SERVER_ERROR("An unexpected internal error occurred", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String defaultMessage;
    private final HttpStatus httpStatus;

    ErrorCode(String defaultMessage, HttpStatus httpStatus) {
        this.defaultMessage = defaultMessage;
        this.httpStatus = httpStatus;
    }
}
