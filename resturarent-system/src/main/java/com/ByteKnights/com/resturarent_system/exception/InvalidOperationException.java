package com.ByteKnights.com.resturarent_system.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class InvalidOperationException extends RuntimeException {
    private final HttpStatus status = HttpStatus.BAD_REQUEST;

    public InvalidOperationException(String message) {
        super(message);
    }
}
