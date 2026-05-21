package com.nhom3.shared.network.payload;

public class ScreenNotifyPayload {
    private int auctionId;
    private double highestPrice;
    private int highestBidderId;
    private String eventType;
    private String message;
    private String status;
    private String endTime;
    private BidHistoryResponsePayload.SimpleBid latestBid;

    public ScreenNotifyPayload(double highestPrice) {
        this(0, highestPrice);
    }

    public ScreenNotifyPayload(int auctionId, double highestPrice) {
        this(auctionId, highestPrice, -1, "PRICE_UPDATED", null, null, null, null);
    }

    public ScreenNotifyPayload(int auctionId, double highestPrice, String eventType, String message, String status) {
        this(auctionId, highestPrice, -1, eventType, message, status, null, null);
    }

    public ScreenNotifyPayload(
            int auctionId,
            double highestPrice,
            int highestBidderId,
            String eventType,
            String message,
            String status,
            String endTime,
            BidHistoryResponsePayload.SimpleBid latestBid) {
        this.auctionId = auctionId;
        this.highestPrice = highestPrice;
        this.highestBidderId = highestBidderId;
        this.eventType = eventType;
        this.message = message;
        this.status = status;
        this.endTime = endTime;
        this.latestBid = latestBid;
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

    public int getHighestBidderId() {
        return highestBidderId;
    }

    public void setHighestBidderId(int highestBidderId) {
        this.highestBidderId = highestBidderId;
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

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public BidHistoryResponsePayload.SimpleBid getLatestBid() {
        return latestBid;
    }

    public void setLatestBid(BidHistoryResponsePayload.SimpleBid latestBid) {
        this.latestBid = latestBid;
    }
}
