package com.wellsfargo.signaturestudio.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when an account key fails validation (format, length, reserved words, etc.).
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidAccountKeyException extends RuntimeException {

    private final String accountKey;

    public InvalidAccountKeyException(String message) {
        super(message);
        this.accountKey = null;
    }

    public InvalidAccountKeyException(String message, String accountKey) {
        super(message);
        this.accountKey = accountKey;
    }

    public String getAccountKey() {
        return accountKey;
    }
}
