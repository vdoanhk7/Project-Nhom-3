package com.nhom3.shared.model.User;
import java.time.LocalDateTime;

import com.nhom3.shared.model.Auction.Auction;
import com.nhom3.shared.model.Auction.BidTransaction;

public class Bidder extends User {
    public Bidder(int id, String userName, String password, String name, String email, String phoneNumber) {
        super(id, userName, password, name, email, phoneNumber);
    }

    public void createBidTransaction(String id, Auction auction, double amount, String note) {
        if (auction == null) {
            System.out.println("Auction is null. Cannot create bid transaction.");
            return;
        }
        BidTransaction bidTransaction = new BidTransaction(id, this, amount, LocalDateTime.now(), note);
        auction.addBid(bidTransaction);
    }
    
        
        
}
