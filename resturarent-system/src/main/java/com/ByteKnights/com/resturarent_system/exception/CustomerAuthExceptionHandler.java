package com.ByteKnights.com.resturarent_system.exception;

import com.ByteKnights.com.resturarent_system.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CustomerAuthExceptionHandler {

    @ExceptionHandler(CustomerAuthException.class)
    public ResponseEntity<ApiResponse<Object>> handleCustomerAuthException(CustomerAuthException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(QrSessionException.class)
    public ResponseEntity<ApiResponse<Object>> handleQrSessionException(QrSessionException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Internal server error"));
    }
}
