package com.nhom3.shared.network.payload;

public class TransactionActionPayload {
    private final int auctionId;
    private final int userId;

    public TransactionActionPayload(int auctionId, int userId) {
        this.auctionId = auctionId;
        this.userId = userId;
    }

    public int getAuctionId() {
        return auctionId;
    }

    public int getUserId() {
        return userId;
    }
}
