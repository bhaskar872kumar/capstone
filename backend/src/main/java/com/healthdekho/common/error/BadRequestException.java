package com.healthdekho.common.error;

import org.springframework.http.HttpStatus;

/** 400: the request itself is not valid. */
public class BadRequestException extends ApiException {

    public BadRequestException(String code, String message) {
        super(HttpStatus.BAD_REQUEST, code, message);
    }
}
