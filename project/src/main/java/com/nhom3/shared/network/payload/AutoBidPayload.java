package com.nhom3.shared.network.payload;

public class AutoBidPayload {
    private int userId;
    private int auctionId;
    private double maxAmount;
    private double increment;
    private String timestamp; // ĐỔI TỪ LocalDateTime SANG String ĐỂ TRÁNH LỖI GSON

    // Constructor
    public AutoBidPayload(int userId, int auctionId, double maxAmount, double increment) {
        this.userId = userId;
        this.auctionId = auctionId;
        this.maxAmount = maxAmount;
        this.increment = increment;
        this.timestamp = java.time.LocalDateTime.now().toString(); // Tự ép thành chuỗi chuẩn ISO
    }

    // Getters
    public int getUserId() {
        return userId;
    }
    public int getAuctionId() {
        return auctionId;
    }
    public double getMaxAmount() {
        return maxAmount;
    }
    public double getIncrement() {
        return increment;
    }
    public String getTimestamp() {
        return timestamp;
    }

    //Setters
    public void setUserId(int userId) {
        this.userId = userId;
    }
    public void setAuctionId(int auctionId) {
        this.auctionId = auctionId;
    }
    public void setMaxAmount(double maxAmount) {
        this.maxAmount = maxAmount;
    }
    public void setIncrement(double increment) {
        this.increment = increment;
    }
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}