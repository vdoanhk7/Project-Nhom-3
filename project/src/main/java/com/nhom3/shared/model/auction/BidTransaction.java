package com.nhom3.shared.model.auction;
import com.nhom3.shared.model.Entity;
import com.nhom3.shared.model.user.Bidder;

import java.time.LocalDateTime;

public class BidTransaction extends Entity {
    private double amount; // tien dat
    private LocalDateTime time; // thoi gian dat
    private String note;
    private Bidder bidder;

    public BidTransaction(int id, Bidder bidder, double amount, LocalDateTime time, String note) {
        super(id);
        this.bidder = bidder;
        this.amount = amount;
        this.time = time;
        this.note = note;
    }


    public double getAmount() {
        return amount;
    }
    public LocalDateTime getBidTime() {
        return time;
    }
    public Bidder getBidder() {
        return bidder;
    }
    public String getNote() {
        return note;
    }


}
