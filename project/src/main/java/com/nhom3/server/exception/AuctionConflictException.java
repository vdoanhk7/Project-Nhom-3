package com.nhom3.server.exception;

public class AuctionConflictException extends BusinessRuleException {
    public AuctionConflictException(String errorCode, String message) {
        super(errorCode, message);
    }
}
