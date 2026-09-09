package com.enterprise.common.exception;

import com.enterprise.common.dto.ApiResponse;
import com.enterprise.common.dto.ErrorDetail;
import com.enterprise.common.filter.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<ErrorDetail> errorDetails = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errorDetails.add(ErrorDetail.builder()
                    .field(fieldError.getField())
                    .code(fieldError.getCode())
                    .detail(fieldError.getDefaultMessage())
                    .build());
        }

        ApiResponse<Void> response = ApiResponse.fail("Validation failed", errorDetails);
        populateMetadata(response, request);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException ex, HttpServletRequest request) {
        log.warn("API exception occurred: code={}, message={}", ex.getErrorCode(), ex.getMessage());
        ApiResponse<Void> response = ApiResponse.fail(ex.getMessage(), ex.getErrors());
        populateMetadata(response, request);
        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        ApiResponse<Void> response = ApiResponse.fail("Invalid email or password");
        populateMetadata(response, request);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        ApiResponse<Void> response = ApiResponse.fail("Access denied. Insufficient permissions");
        populateMetadata(response, request);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGlobalException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception processing request: path={}", request.getRequestURI(), ex);
        ApiResponse<Void> response = ApiResponse.fail("An unexpected internal error occurred");
        populateMetadata(response, request);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private void populateMetadata(ApiResponse<?> response, HttpServletRequest request) {
        response.setPath(request.getRequestURI());
        String correlationId = (String) request.getAttribute(CorrelationIdFilter.CORRELATION_ID_KEY);
        if (correlationId == null) {
            correlationId = request.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        }
        response.setCorrelationId(correlationId);
    }
}
