package com.nhom3.server.exception;

public class InvalidUserDataException extends BusinessValidationException {
    public InvalidUserDataException(String errorCode, String message) {
        super(errorCode, message);
    }
}
