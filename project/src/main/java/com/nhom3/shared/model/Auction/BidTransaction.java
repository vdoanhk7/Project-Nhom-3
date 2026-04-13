package com.nhom3.shared.model.Auction;
import com.nhom3.shared.model.Entity;
import java.time.LocalDateTime;
import com.nhom3.shared.model.User.Bidder;

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
