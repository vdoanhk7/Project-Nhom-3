package com.nhom3.server.dao;
import com.nhom3.shared.model.auction.BidTransaction;

import java.util.Map;

import com.nhom3.shared.model.auction.Auction;

public interface AuctionDAO {
    boolean updateHighestBid(int auctionId, int bidderId, double newAmount);
    boolean saveBidTransaction(BidTransaction bid, int auctionId);
    void closeExpiredAuctions();
    boolean createAuction(Auction auction);
    Map<Integer, String> getAuctionStatusBySeller(int sellerId);
    Auction getAuctionByItemId(int itemId);
    boolean confirmPayment(int auctionId);
}