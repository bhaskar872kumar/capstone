package com.healthdekho.common.error;

import org.springframework.http.HttpStatus;

/** 404: the record doesn't exist, or it belongs to another customer. */
public class NotFoundException extends ApiException {

    public NotFoundException(String code, String message) {
        super(HttpStatus.NOT_FOUND, code, message);
    }
}
