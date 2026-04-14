package com.nhom3.sever.dao;
import com.nhom3.shared.model.auction.BidTransaction;

public interface AuctionDAO {
    boolean updateHighestBid(int auctionId, int bidderId, double newAmount);
    boolean saveBidTransaction(BidTransaction bid, int auctionId);
    void closeExpiredAuctions();
}