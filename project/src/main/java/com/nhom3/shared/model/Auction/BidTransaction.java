package com.nhom3.shared.model.Auction;

import java.time.LocalDateTime;
import com.nhom3.shared.model.User.Bidder;

public class BidTransaction {
    private String id;
    private double bidAmount; // tien dat
    private LocalDateTime bidTime; // thoi gian dat
    private String note;
    private Bidder bidder;

    public BidTransaction(String id, Bidder bidder, double bidAmount, LocalDateTime bidTime, String note) {
        this.id = id;
        this.bidder = bidder;
        this.bidAmount = bidAmount;
        this.bidTime = bidTime;
        this.note = note;
    }

    public String getId() {
        return id;
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
    public void setId(String id) {
        this.id = id;
    }

}
