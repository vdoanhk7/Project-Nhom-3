package com.nhom3.server.exception;

public class ItemOperationException extends BusinessRuleException {
    public ItemOperationException(String errorCode, String message) {
        super(errorCode, message);
    }
}
