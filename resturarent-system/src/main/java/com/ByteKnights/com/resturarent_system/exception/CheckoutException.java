package com.ByteKnights.com.resturarent_system.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CheckoutException extends RuntimeException{
    private final HttpStatus status;

    public CheckoutException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
