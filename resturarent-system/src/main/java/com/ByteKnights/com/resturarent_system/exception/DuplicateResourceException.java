package com.ByteKnights.com.resturarent_system.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class DuplicateResourceException extends RuntimeException {
    private final HttpStatus status = HttpStatus.CONFLICT;

    public DuplicateResourceException(String message) {
        super(message);
    }
}
