package com.healthdekho.common.error;

import org.springframework.http.HttpStatus;

/** 409: the same thing already exists (duplicate policy, installment already paid). */
public class DuplicateResourceException extends ApiException {

    public DuplicateResourceException(String code, String message) {
        super(HttpStatus.CONFLICT, code, message);
    }
}
