package com.ByteKnights.com.resturarent_system.exception;

import com.ByteKnights.com.resturarent_system.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.access.AccessDeniedException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CustomerAuthException.class)
    public ResponseEntity<ApiResponse<Object>> handleCustomerAuthException(CustomerAuthException ex) {
        log.warn("Customer auth error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(ex.getStatus())
                .body(ApiResponse.error(ex.getMessage()));
    }
    @ExceptionHandler(QrSessionException.class)
    public ResponseEntity<ApiResponse<Object>> handleQrSessionException(QrSessionException ex) {
        log.warn("QR session error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(ex.getStatus())
                .body(ApiResponse.error(ex.getMessage()));
    }

    //Used when resource not found, user, menu, branch not found
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage(), ex);
        return ResponseEntity.status(ex.getStatus())
                .body(ApiResponse.error(ex.getMessage()));
    }

    //Used when resource already exists, duplicate resources
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Object>> handleDuplicateResourceException(DuplicateResourceException ex) {
        log.warn("Duplicate resource: {}", ex.getMessage(), ex);
        return ResponseEntity.status(ex.getStatus())
                .body(ApiResponse.error(ex.getMessage()));
    }

    //Used when invalid operation is performed, violates business logic
    @ExceptionHandler(InvalidOperationException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidOperationException(InvalidOperationException ex) {
        log.warn("Invalid operation: {}", ex.getMessage(), ex);
        return ResponseEntity.status(ex.getStatus())
                .body(ApiResponse.error(ex.getMessage()));
    }

    //Used when user tries to access something they don't have permission to access
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDeniedException(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Access Denied"));
    }

    //Used when runtime exception occurs
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Object>> handleRuntimeException(RuntimeException ex) {
        log.error("Unhandled runtime exception: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Internal server error: " + ex.getMessage()));
    }
}