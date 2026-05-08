package com.nhom3.server.dao;
import com.nhom3.shared.model.auction.BidTransaction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.sql.Timestamp;
import com.nhom3.shared.network.payload.AutoBidPayload;
import com.nhom3.shared.network.payload.DashboardResponsePayload;
import com.nhom3.shared.model.auction.Auction;

public interface AuctionDAO {
    boolean updateHighestBid(int auctionId, int bidderId, double newAmount);
    boolean saveBidTransaction(BidTransaction bid, int auctionId);
    boolean createAuction(Auction auction);
    Map<Integer, String> getAuctionStatusBySeller(int sellerId);
    Auction getAuctionByItemId(int itemId);
    boolean confirmPayment(int auctionId);
    List<Auction> getActiveAuctions();
    List<BidTransaction> getBidHistory(int auctionId);
    List<Auction> getMyBidHistory(int bidderId);
    boolean extendAuctionTime(int auctionId, int additionalMinutes);
    boolean cancelAuction(int auctionId);
    LocalDateTime getEndTime(int auctionId);
    boolean endAuction(int auctionId);
    boolean startAuction(int auctionId);
    void closeExpiredAuctions(Timestamp currentTime); 
    void startScheduledAuctions(Timestamp currentTime); 
    boolean saveAutoBidConfig(AutoBidPayload payload);
    DashboardResponsePayload getDashboardStats();
    Auction getAuctionById(int auctionId);
    List<AutoBidPayload> getActiveAutoBids(int auctionId);
    AutoBidPayload getUserAutoBid(int auctionId, int userId);
    boolean cancelAutoBid(int auctionId, int userId);
}