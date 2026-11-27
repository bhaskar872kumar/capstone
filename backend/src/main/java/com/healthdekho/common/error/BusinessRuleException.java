package com.healthdekho.common.error;

import org.springframework.http.HttpStatus;

/** 422: the request is well formed but breaks a business rule (age, amount above outstanding...). */
public class BusinessRuleException extends ApiException {

    public BusinessRuleException(String code, String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, code, message);
    }
}
