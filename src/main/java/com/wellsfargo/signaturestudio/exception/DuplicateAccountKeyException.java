package com.wellsfargo.signaturestudio.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when attempting to create an account with a key that already exists.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateAccountKeyException extends RuntimeException {

    private final String accountKey;

    public DuplicateAccountKeyException(String message) {
        super(message);
        this.accountKey = null;
    }

    public DuplicateAccountKeyException(String message, String accountKey) {
        super(message);
        this.accountKey = accountKey;
    }

    public String getAccountKey() {
        return accountKey;
    }
}
