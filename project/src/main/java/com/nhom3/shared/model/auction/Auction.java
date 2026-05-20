package com.nhom3.shared.model.auction;
import java.time.LocalDateTime;
import com.nhom3.shared.model.Entity;
import com.nhom3.shared.model.item.Item;
import com.nhom3.shared.model.user.Bidder;

import java.util.ArrayList;
import java.util.List;

public class Auction extends Entity{
    public static final double DEFAULT_BID_STEP = 50000;

    private Item item;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private double bidStep;
    private Bidder highestBidder;
    private StatusOfAuction status;
    private List<BidTransaction> bidHistory;
    private int sellerRatingByCurrentBuyer;
    
    public Auction(int id, Item item, LocalDateTime startTime, LocalDateTime endTime) {
        super(id);
        this.item = item;
        this.startTime = startTime;
        this.endTime = endTime;
        this.bidStep = DEFAULT_BID_STEP;
        this.status = StatusOfAuction.OPEN;
        this.bidHistory = new ArrayList<>();
        this.sellerRatingByCurrentBuyer = -1;
        
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

    public void setBidStep(double bidStep) {
        this.bidStep = bidStep;
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

    public double getBidStep() {
        return bidStep;
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
    public int getHighestBidderId() {
        return highestBidder != null ? highestBidder.getId() : -1;
    }

    public int getSellerRatingByCurrentBuyer() {
        return sellerRatingByCurrentBuyer;
    }

    public void setSellerRatingByCurrentBuyer(int sellerRatingByCurrentBuyer) {
        if (sellerRatingByCurrentBuyer < 0) {
            this.sellerRatingByCurrentBuyer = -1;
            return;
        }
        this.sellerRatingByCurrentBuyer = Math.min(5, sellerRatingByCurrentBuyer);
    }

    @Override
    public String printInfo() {
        String itemName = item == null ? "N/A" : item.getName();
        return String.format(
                "Auction[id=%d, item=%s, startTime=%s, endTime=%s, bidStep=%.0f, status=%s, highestBidderId=%d]",
                id,
                itemName,
                startTime,
                endTime,
                bidStep,
                status,
                getHighestBidderId());
    }
    
}
