package com.nhom3.server.exception;

public class UserOperationException extends BusinessRuleException {
    public UserOperationException(String errorCode, String message) {
        super(errorCode, message);
    }
}
