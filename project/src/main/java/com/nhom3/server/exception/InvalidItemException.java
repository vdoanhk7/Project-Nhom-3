package com.nhom3.server.exception;

public class InvalidItemException extends BusinessValidationException {
    public InvalidItemException(String errorCode, String message) {
        super(errorCode, message);
    }
}
