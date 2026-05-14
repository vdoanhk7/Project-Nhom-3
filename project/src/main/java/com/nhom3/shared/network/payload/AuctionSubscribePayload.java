package com.nhom3.shared.network.payload;

public class AuctionSubscribePayload {
    private int auctionId;
    private boolean Subed;

    public AuctionSubscribePayload(int auctionId, boolean Subed) {
        this.auctionId = auctionId;
        this.Subed = Subed;
    }

    public int getAuctionId() {
        return auctionId;
    }

    public boolean isSub() {
        return Subed;
    }

}