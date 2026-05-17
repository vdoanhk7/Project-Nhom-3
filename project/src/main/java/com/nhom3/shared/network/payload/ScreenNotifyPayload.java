package com.nhom3.shared.network.payload;

public class ScreenNotifyPayload {
    private int auctionId;
    private double highestPrice;
    private String eventType;
    private String message;
    private String status;

    public ScreenNotifyPayload(double highestPrice) {
        this(0, highestPrice);
    }

    public ScreenNotifyPayload(int auctionId, double highestPrice) {
        this(auctionId, highestPrice, "PRICE_UPDATED", null, null);
    }

    public ScreenNotifyPayload(int auctionId, double highestPrice, String eventType, String message, String status) {
        this.auctionId = auctionId;
        this.highestPrice = highestPrice;
        this.eventType = eventType;
        this.message = message;
        this.status = status;
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

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
