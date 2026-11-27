package com.healthdekho.common.error;

import org.springframework.http.HttpStatus;

/** 401: not logged in, or the login details are wrong. */
public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String code, String message) {
        super(HttpStatus.UNAUTHORIZED, code, message);
    }
}
