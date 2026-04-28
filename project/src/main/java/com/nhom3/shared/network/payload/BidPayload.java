package com.nhom3.shared.network.payload;

import java.time.LocalDateTime;

public class BidPayload {
    private int userId;
    private double amount;
    private int auctionId;
    private LocalDateTime timestamp;

    // Constructor
    public BidPayload(int userId, double amount, int auctionId) {
        this.userId = userId;
        this.amount = amount;
        this.auctionId = auctionId;
        this.timestamp = LocalDateTime.now();
    }

    // Getters
    public int getUserId() {
        return userId;
    }
    public double getAmount() {
        return amount;
    }
    public int getAuctionId() {
        return auctionId;
    }
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    //Setters
    public void setUserId(int userId) {
        this.userId = userId;
    }
    public void setAmount(double amount) {
        this.amount = amount;
    }
    public void setAuctionId(int auctionId) {
        this.auctionId = auctionId;
    }
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}