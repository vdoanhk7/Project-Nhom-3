package com.nhom3.server.exception;

public class AuctionNotFoundException extends BusinessRuleException {
    public AuctionNotFoundException(String errorCode, String message) {
        super(errorCode, message);
    }
}
