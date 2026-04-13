package com.nhom3.shared.model.auction;
import com.nhom3.shared.model.Entity;
import com.nhom3.shared.model.user.Bidder;

import java.time.LocalDateTime;

public class BidTransaction extends Entity {
    private double bidAmount; // tien dat
    private LocalDateTime bidTime; // thoi gian dat
    private String note;
    private Bidder bidder;

    public BidTransaction(int id, Bidder bidder, double bidAmount, LocalDateTime bidTime, String note) {
        super(id);
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


}
