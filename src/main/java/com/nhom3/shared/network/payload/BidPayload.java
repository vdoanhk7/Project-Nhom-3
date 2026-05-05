package com.nhom3.shared.network.payload;

public class BidPayload {
    private int userId;
    private double amount;
    private int auctionId;
    private String timestamp; // ĐỔI TỪ LocalDateTime SANG String

    // Constructor
    public BidPayload(int userId, double amount, int auctionId) {
        this.userId = userId;
        this.amount = amount;
        this.auctionId = auctionId;
        // Tự động lấy giờ hiện tại và chuyển thành String chuẩn ISO
        this.timestamp = java.time.LocalDateTime.now().toString(); 
    }

    // Getters
    public int getUserId() { return userId; }
    public double getAmount() { return amount; }
    public int getAuctionId() { return auctionId; }
    public String getTimestamp() { return timestamp; }

    // Setters
    public void setUserId(int userId) { this.userId = userId; }
    public void setAmount(double amount) { this.amount = amount; }
    public void setAuctionId(int auctionId) { this.auctionId = auctionId; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}