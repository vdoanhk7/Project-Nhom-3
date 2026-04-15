package com.nhom3.server.dao;

import com.nhom3.server.db.DbConnection;
import com.nhom3.shared.model.auction.BidTransaction;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;

public class AuctionDAOImpl implements AuctionDAO {

    @Override
    public boolean updateHighestBid(int auctionId, int bidderId, double newAmount) {
        String sql = "UPDATE items SET cur_highest = ? WHERE id = (SELECT item_id FROM auctions WHERE id = ?) AND cur_highest < ?";
        String sqlAuction = "UPDATE auctions SET highest_bidder_id = ? WHERE id = ?";

        try (Connection conn = DbConnection.getInstance()) {
            conn.setAutoCommit(false);

            try (PreparedStatement stmt1 = conn.prepareStatement(sql)) {
                stmt1.setDouble(1, newAmount);
                stmt1.setInt(2, auctionId);
                stmt1.setDouble(3, newAmount);
                
                int rowsUpdated = stmt1.executeUpdate();
                if (rowsUpdated == 0) {
                    conn.rollback();
                    return false; 
                }
            }
            try (PreparedStatement stmt2 = conn.prepareStatement(sqlAuction)) {
                stmt2.setInt(1, bidderId);
                stmt2.setInt(2, auctionId);
                stmt2.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean saveBidTransaction(BidTransaction bid, int auctionId) {
        String sql = "INSERT INTO bid_transactions (auction_id, bidder_id, amount, bid_time, note) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DbConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, auctionId);
            stmt.setInt(2, bid.getBidder().getId());
            stmt.setDouble(3, bid.getAmount());
            stmt.setTimestamp(4, Timestamp.valueOf(bid.getBidTime()));
            stmt.setString(5, bid.getNote());
            
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    public void closeExpiredAuctions() {
    String sql = "UPDATE auctions SET status = 'FINISHED' WHERE status = 'RUNNING' AND end_time <= NOW()";
    
    try (Connection conn = DbConnection.getInstance();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
        
        int rowsUpdated = stmt.executeUpdate();
        if (rowsUpdated > 0) {
            System.out.println("[Hệ thống] Đã tự động đóng " + rowsUpdated + " phiên đấu giá hết hạn!");
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
}
}