package com.nhom3;
import java.util.ArrayList;
import java.time.LocalDate;
import com.nhom3.Item.Item;
import com.nhom3.User.Bidder;


public class Auction extends Entity{
    private Item item;
    private LocalDate startTime;
    private LocalDate endTime;
    private Bidder highestBidder;
    private StatusOfAuction status;
    public Auction(String id) {
        super(id);
    }

    @Override
    public void displayInfo() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'displayInfo'");
    }
    public void addBid(BidTransaction bid) {
        if (status == StatusOfAuction.RUNNING) {
            if (highestBidder == null || bid.getBidAmount() > item.getCurHighest()) {
                highestBidder = bid.getBidder();
                item.setCurHighest(bid.getBidAmount());
            }
        }
    }

}
