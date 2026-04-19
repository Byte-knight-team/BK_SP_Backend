package com.ByteKnights.com.resturarent_system.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class QrSessionException extends RuntimeException {
    private final HttpStatus status;

    public QrSessionException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}