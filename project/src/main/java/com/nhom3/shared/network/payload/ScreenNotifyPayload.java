package com.nhom3.shared.network.payload;

public class ScreenNotifyPayload {
    private int auctionId;
    private double highestPrice;

    public ScreenNotifyPayload(double highestPrice) {
        this(0, highestPrice);
    }

    public ScreenNotifyPayload(int auctionId, double highestPrice) {
        this.auctionId = auctionId;
        this.highestPrice = highestPrice;
    }

    public int getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(int auctionId) {
        this.auctionId = auctionId;
    }

    public double getHighestPrice() {
        return highestPrice;
    }

    public void setHighestPrice(double highestPrice) {
        this.highestPrice = highestPrice;
    }
}
