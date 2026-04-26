package com.nhom3.server.dao;

import com.nhom3.server.db.DbConnection;
import com.nhom3.shared.model.auction.Auction;
import com.nhom3.shared.model.auction.BidTransaction;
import com.nhom3.shared.model.auction.StatusOfAuction;
import com.nhom3.shared.model.user.Bidder;
import com.nhom3.shared.model.user.UserInfo;
import com.nhom3.shared.model.item.Item;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuctionDAOImpl implements AuctionDAO {

    @Override
    public boolean updateHighestBid(int auctionId, int bidderId, double newAmount) {
        String sql = "UPDATE items i " +
                    "JOIN auctions a ON i.id = a.item_id " +
                    "SET i.cur_highest = ? " +
                    "WHERE a.id = ? " +
                    "AND i.cur_highest < ? " +
                    "AND NOW() BETWEEN a.start_time AND a.end_time";

        String sqlAuction = "UPDATE auctions SET highest_bidder_id = ? WHERE id = ?";

        try (Connection conn = DbConnection.getConnection()) {
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
        try (Connection conn = DbConnection.getConnection();
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
    
    try (Connection conn = DbConnection.getConnection();
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
        try (Connection conn = DbConnection.getConnection();
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

    @Override
    public Map<Integer, String> getAuctionStatusBySeller(int sellerId) {
        Map<Integer, String> statusMap = new HashMap<>();
        String sql = "SELECT a.item_id, " +
                     "CASE " +
                     "   WHEN a.status IN ('PAID', 'CANCELLED') THEN a.status " +
                     "   WHEN ? >= a.end_time THEN 'FINISHED' " +
                     "   WHEN ? >= a.start_time THEN 'RUNNING' " +
                     "   ELSE 'OPEN' " +
                     "END AS real_status " +
                     "FROM auctions a " +
                     "JOIN items i ON a.item_id = i.id " +
                     "WHERE i.seller_id = ?";     
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) { 
            java.sql.Timestamp currentTime = java.sql.Timestamp.valueOf(LocalDateTime.now());
            stmt.setTimestamp(1, currentTime);
            stmt.setTimestamp(2, currentTime);
            stmt.setInt(3, sellerId);
            
            ResultSet rs = stmt.executeQuery();           
            while (rs.next()) {
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
        
        try (Connection conn = DbConnection.getConnection();
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
        try (Connection conn = DbConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {  
            stmt.setInt(1, auctionId);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public List<Auction> getActiveAuctions() {
        List<Auction> list = new ArrayList<>();
        String sql = "SELECT a.*, i.name, i.item_type, i.start_price, i.cur_highest " +
                    "FROM auctions a " +
                    "JOIN items i ON a.item_id = i.id " +
                    "WHERE a.end_time > ? AND a.status != 'CANCELLED' " +
                    "ORDER BY a.end_time ASC";

        try (Connection conn = DbConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, java.sql.Timestamp.valueOf(LocalDateTime.now()));     
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Item item = new Item(
                        rs.getInt("item_id"),
                        rs.getString("name"),
                        rs.getDouble("start_price"),
                        rs.getString("item_type") 
                    ) {};       
                    item.setCurHighest(rs.getDouble("cur_highest"));
                    int id = rs.getInt("id");
                    java.time.LocalDateTime start = rs.getTimestamp("start_time").toLocalDateTime();
                    java.time.LocalDateTime end = rs.getTimestamp("end_time").toLocalDateTime();
                    Auction auction = new Auction(id, item, start, end);
                    
                    java.time.LocalDateTime now = java.time.LocalDateTime.now();
                    if (now.isBefore(start)) {
                        auction.setStatus(StatusOfAuction.OPEN);
                    } else {
                        auction.setStatus(StatusOfAuction.RUNNING);
                    }
                    list.add(auction);
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi tải danh sách Chợ Đấu Giá: ");
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public List<BidTransaction> getBidHistory(int auctionId) {
        List<BidTransaction> list = new ArrayList<>();
        
        String sql = "SELECT b.id, b.amount, b.bid_time, b.note, b.bidder_id, u.full_name AS bidder_name " +
                     "FROM bid_transactions b " +
                     "LEFT JOIN users u ON b.bidder_id = u.id " +
                     "WHERE b.auction_id = ? " +
                     "ORDER BY b.bid_time DESC";
                     
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
             
            stmt.setInt(1, auctionId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) { 
                // rs.getString("bidder_name") sẽ lấy ra giá trị của cột u.full_name
                UserInfo userInfo = new UserInfo("","", rs.getString("bidder_name"));
                Bidder bidder = new Bidder(rs.getInt("bidder_id"), userInfo, null); 

                BidTransaction bid = new BidTransaction(
                    rs.getInt("id"),
                    bidder,
                    rs.getDouble("amount"),
                    rs.getTimestamp("bid_time").toLocalDateTime(),
                    rs.getString("note")
                );              
                list.add(bid);
            }
        } catch (Exception e) {
            System.err.println("❌ Lỗi khi tải Lịch sử trả giá: ");
            e.printStackTrace(); 
        }
        return list;
    }

    @Override
    public List<Auction> getMyBidHistory(int bidderId) {
        List<Auction> list = new ArrayList<>();
        String sql = "SELECT a.id AS auction_id, a.highest_bidder_id, a.start_time, a.end_time, " +
                     "i.id AS item_id, i.name AS item_name, " +
                     "b.amount, b.bid_time, " +
                     "CASE " +
                     "   WHEN a.status IN ('PAID', 'CANCELLED') THEN a.status " +
                     "   WHEN NOW() >= a.end_time THEN 'FINISHED' " +
                     "   WHEN NOW() >= a.start_time THEN 'RUNNING' " +
                     "   ELSE 'OPEN' " +
                     "END AS real_status " +
                     "FROM bid_transactions b " +
                     "JOIN auctions a ON b.auction_id = a.id " +
                     "JOIN items i ON a.item_id = i.id " +
                     "WHERE b.bidder_id = ? " +
                     "ORDER BY b.bid_time DESC";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {         
            stmt.setInt(1, bidderId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Item item = new Item(rs.getInt("item_id"), rs.getString("item_name"), 0, "") {};
                java.time.LocalDateTime start = rs.getTimestamp("start_time").toLocalDateTime();
                java.time.LocalDateTime end = rs.getTimestamp("end_time").toLocalDateTime();
                Auction auction = new Auction(rs.getInt("auction_id"), item, start, end);
                try {
                    auction.setStatus(StatusOfAuction.valueOf(rs.getString("real_status")));
                } catch (Exception e) {
                    auction.setStatus(StatusOfAuction.OPEN);
                }
                Bidder topBidder = new Bidder(rs.getInt("highest_bidder_id"), null, null);
                auction.setHighestBidder(topBidder);
                BidTransaction myBid = new BidTransaction(0, null, rs.getDouble("amount"), rs.getTimestamp("bid_time").toLocalDateTime(), "");
                auction.getBidHistory().add(myBid);
                list.add(auction);
            }
        } catch (Exception e) { 
            System.err.println("Lỗi khi tải lịch sử cá nhân: ");
            e.printStackTrace(); 
        }
        return list;
    }

    @Override
    public boolean extendAuctionTime(int auctionId, int additionalMinutes) {
        String sql = "UPDATE auctions SET end_time = DATE_ADD(end_time, INTERVAL ? MINUTE) WHERE id = ?";
        try (Connection conn = DbConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, additionalMinutes);
            stmt.setInt(2, auctionId);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
}