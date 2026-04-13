package com.nhom3.Auction;

import java.time.LocalDateTime;
import com.nhom3.Entity;
import com.nhom3.User.Bidder;

public class BidTransaction extends Entity{
    private double bidAmount; // tien dat
    private LocalDateTime bidTime; // thoi gian dat
    private String note;
    private Bidder bidder;

    public BidTransaction(String id, Bidder bidder, double bidAmount, LocalDateTime bidTime, String note) {
        super("BidTransaction-" + id);
        this.bidder = bidder;
        this.bidAmount = bidAmount;
        this.bidTime = bidTime;
        this.note = note;
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
        System.out.println("Giá đặt: " + bidAmount);
        System.out.println("Người đặt: " + bidder.getName());
        System.out.println("Thời gian đặt: " + bidTime);
        System.out.println("Ghi chú: " + note);
    }

}
