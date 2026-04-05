package com.nhom3;

import java.time.LocalDateTime;
import com.nhom3.User.Bidder;

class BidTransaction extends Entity{
    private String auctionId; // phien dau gia
    private double bidAmount; // tien dat
    private LocalDateTime bidTime; // thoi gian dat
    private String note;
    private Bidder bidder;

    public BidTransaction(String id, String auctionId, Bidder bidder, double bidAmount,
                          LocalDateTime bidTime, String note) {
        super(id);
        this.auctionId = auctionId;
        this.bidAmount = bidAmount;
        this.bidTime = bidTime;
        this.note = note;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public double getBidAmount() {
        return bidAmount;
    }

    public LocalDateTime getBidTime() {
        return bidTime;
    }

    public Bidder getBidder() {
        return bidder;
    }

    public String getNote() {
        return note;
    }

    @Override
    public void displayInfo() {
        System.out.println("Mã giao dịch: " + id);
        System.out.println("Phiên đấu giá: " + auctionId);
        System.out.println("Giá đặt: " + bidAmount);
        System.out.println("Thời gian đặt: " + bidTime);
        System.out.println("Ghi chú: " + note);
    }

}
