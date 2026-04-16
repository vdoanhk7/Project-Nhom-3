package com.nhom3.server.dao;

import com.nhom3.server.db.DbConnection;
import com.nhom3.shared.model.auction.Auction;
import com.nhom3.shared.model.auction.BidTransaction;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

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
    @Override
    public boolean createAuction(Auction auction) {
        String sql = "INSERT INTO auctions (item_id, start_time, end_time, status) VALUES (?, ?, ?, ?)";
        try (Connection conn = DbConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, auction.getItem().getId());

            stmt.setTimestamp(2, Timestamp.valueOf(auction.getStartTime()));
            stmt.setTimestamp(3, Timestamp.valueOf(auction.getEndTime())); 
            stmt.setString(4, auction.getStatus().name()); 
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (java.sql.ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        auction.setId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public Map<Integer, String> getAuctionStatusBySeller(int sellerId) {
        Map<Integer, String> statusMap = new HashMap<>();
        // DÙNG CÚ PHÁP 'CASE WHEN' ĐỂ MYSQL TỰ SO SÁNH GIỜ THỰC TẾ (không thực sự cập nhật vào cột status trong database)
        String sql = "SELECT a.item_id, " +
                     "CASE " +
                     "   WHEN a.status IN ('PAID', 'CANCELLED') THEN a.status" +
                     "   WHEN NOW() >= a.end_time THEN 'FINISHED' " +
                     "   WHEN NOW() >= a.start_time THEN 'RUNNING' " +
                     "   ELSE 'OPEN' " +
                     "END AS real_status " +
                     "FROM auctions a " +
                     "JOIN items i ON a.item_id = i.id " +
                     "WHERE i.seller_id = ?";
        try (Connection conn = DbConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) { 
            stmt.setInt(1, sellerId);
            ResultSet rs = stmt.executeQuery();           
            while (rs.next()) {
                // Đọc cột real_status thay vì cột status cũ
                statusMap.put(rs.getInt("item_id"), rs.getString("real_status"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return statusMap;
    }

    @Override
    public Auction getAuctionByItemId(int itemId) {
        // Lấy phiên đấu giá mới nhất của sản phẩm này
        String sql = "SELECT * FROM auctions WHERE item_id = ? ORDER BY id DESC LIMIT 1";
        
        try (Connection conn = DbConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, itemId);
            ResultSet rs = stmt.executeQuery();      
            if (rs.next()) {
                int id = rs.getInt("id");
                java.time.LocalDateTime start = rs.getTimestamp("start_time").toLocalDateTime();
                java.time.LocalDateTime end = rs.getTimestamp("end_time").toLocalDateTime();           
                // Khởi tạo Auction (Truyền null cho thuộc tính Item để tránh query vòng lặp, vì bên giao diện ta chỉ cần lấy thời gian)
                return new Auction(id, null, start, end); 
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean confirmPayment(int auctionId) {
        String sql = "UPDATE auctions SET status = 'PAID' WHERE id = ?";      
        try (Connection conn = DbConnection.getInstance();
            PreparedStatement stmt = conn.prepareStatement(sql)) {  
            stmt.setInt(1, auctionId);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}