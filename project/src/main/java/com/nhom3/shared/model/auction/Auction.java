package com.nhom3.shared.model.auction;
import java.time.LocalDateTime;
import com.nhom3.shared.model.Entity;
import com.nhom3.shared.model.item.Item;
import com.nhom3.shared.model.user.Bidder;

import java.util.ArrayList;
import java.util.List;

public class Auction extends Entity{
    private Item item;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Bidder highestBidder;
    private StatusOfAuction status;
    private List<BidTransaction> bidHistory;
    
    public Auction(int id, Item item, LocalDateTime startTime, LocalDateTime endTime) {
        super(id);
        this.item = item;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = StatusOfAuction.OPEN;
        this.bidHistory = new ArrayList<>();
        
    }


    public void setItem(Item item) {
        this.item = item;
    }


    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }


    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }


    public void setHighestBidder(Bidder highestBidder) {
        this.highestBidder = highestBidder;
    }


    public void setBidHistory(List<BidTransaction> bidHistory) {
        this.bidHistory = bidHistory;
    }


    public Item getItem() {
        return item;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public Bidder getHighestBidder() {
        return highestBidder;
    }

    public StatusOfAuction getStatus() {
        return status;
    }

    public void setStatus(StatusOfAuction status) {
        this.status = status;
    }

    public List<BidTransaction> getBidHistory() {
        return bidHistory;
    }

    
}
