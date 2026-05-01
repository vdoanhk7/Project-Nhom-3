package com.nhom3.shared.network.payload;

public class AuctionIdPayload {
    private double auctionId; // Để double cho Gson đỡ bị lỗi
    
    public AuctionIdPayload(int auctionId) { 
        this.auctionId = (double) auctionId; 
    }
    
    // Khi lấy ra thì ép về int lại
    public int getAuctionId() { 
        return (int) auctionId; 
    }
}