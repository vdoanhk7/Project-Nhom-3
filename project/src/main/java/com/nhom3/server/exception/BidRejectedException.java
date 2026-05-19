package com.nhom3.server.exception;

public class BidRejectedException extends BusinessRuleException {
    public BidRejectedException(String errorCode, String message) {
        super(errorCode, message);
    }
}
