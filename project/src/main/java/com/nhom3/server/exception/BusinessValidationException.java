package com.nhom3.server.exception;

public class BusinessValidationException extends IllegalArgumentException implements DomainException {
    private final String errorCode;

    public BusinessValidationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessValidationException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    @Override
    public String getErrorCode() {
        return errorCode;
    }
}
