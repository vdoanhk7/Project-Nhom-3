package com.nhom3.server.exception;

public class InvalidAuctionException extends BusinessValidationException {
    public InvalidAuctionException(String errorCode, String message) {
        super(errorCode, message);
    }
}
