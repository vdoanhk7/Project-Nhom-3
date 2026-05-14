package com.nhom3.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.mindrot.jbcrypt.BCrypt;

import com.nhom3.server.db.DbConnection;
import com.nhom3.shared.model.user.Admin;
import com.nhom3.shared.model.user.Bidder;
import com.nhom3.shared.model.user.Seller;
import com.nhom3.shared.model.user.User;
import com.nhom3.shared.model.user.UserContact;
import com.nhom3.shared.model.user.UserInfo;

public class UserDAOImpl implements UserDAO {
    private User mapUser(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String role = rs.getString("role");
        UserInfo info = new UserInfo(
                rs.getString("username"),
                rs.getString("password"),
                rs.getString("full_name"));
        UserContact contact = new UserContact(
                rs.getString("email"),
                rs.getString("phone"));
        if (role.equals("BIDDER")) {
            return new Bidder(id, info, contact);
        } else if (role.equals("SELLER")) {
            return new Seller(id, info, contact);
        } else if (role.equals("ADMIN")) {
            return new Admin(id, info, contact);
        }
        return null;
    }

    @Override
    public User login(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DbConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String hashedPassword = rs.getString("password");
                // Kiểm tra mật khẩu bằng BCrypt
                boolean passwordMatch = BCrypt.checkpw(password, hashedPassword);

                if (passwordMatch) {
                    return mapUser(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean register(User user) {
        String sql = "INSERT INTO users (username, password, full_name, email, phone, role) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DbConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getUserInfo().getUserName());
            // Mã hóa mật khẩu trước khi lưu
            stmt.setString(2, BCrypt.hashpw(user.getUserInfo().getPassword(), BCrypt.gensalt()));
            stmt.setString(3, user.getUserInfo().getName());
            stmt.setString(4, user.getUserContact().getEmail());
            stmt.setString(5, user.getUserContact().getPhoneNumber());
            stmt.setString(6, user.getRole().name());
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean updateUser(User user) {
        String sql = "UPDATE users SET full_name = ?, email = ?, phone = ? WHERE id = ?";
        try (Connection conn = DbConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getUserInfo().getName());
            stmt.setString(2, user.getUserContact().getEmail());
            stmt.setString(3, user.getUserContact().getPhoneNumber());
            stmt.setInt(4, user.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean updatePassword(int userId, String newPassword) {
        String sql = "UPDATE users SET password = ? WHERE id = ?";
        try (Connection conn = DbConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            // Mã hóa mật khẩu mới
            stmt.setString(1, BCrypt.hashpw(newPassword, BCrypt.gensalt()));
            stmt.setInt(2, userId); // Tìm đúng ID người dùng để đổi mật khẩu

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean changePassword(int userId, String oldPassword, String newPassword) {
        String sqlSelect = "SELECT password FROM users WHERE id = ?";
        String sqlUpdate = "UPDATE users SET password = ? WHERE id = ?";

        try (Connection conn = DbConnection.getConnection()) {
            String currentHash = null;
            try (PreparedStatement stmt = conn.prepareStatement(sqlSelect)) {
                stmt.setInt(1, userId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        currentHash = rs.getString("password");
                    }
                }
            }

            if (currentHash == null || !BCrypt.checkpw(oldPassword, currentHash)) {
                return false;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sqlUpdate)) {
                stmt.setString(1, BCrypt.hashpw(newPassword, BCrypt.gensalt()));
                stmt.setInt(2, userId);
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY id ASC";

        try (Connection conn = DbConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                User user = mapUser(rs);
                if (user != null) {
                    users.add(user);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    @Override
    public boolean deleteUser(int userId) {
        Connection conn = null;
        try {
            conn = DbConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. Tìm tất cả các phiên đấu giá mà user này đang giữ top 1 (highest_bidder_id)
            String sqlGetAuctions = "SELECT a.id AS auction_id, a.item_id, i.start_price " +
                                    "FROM auctions a JOIN items i ON a.item_id = i.id " +
                                    "WHERE a.highest_bidder_id = ?";
            PreparedStatement stmtGet = conn.prepareStatement(sqlGetAuctions);
            stmtGet.setInt(1, userId);
            ResultSet rsAuctions = stmtGet.executeQuery();
            
            List<Object[]> auctionsList = new ArrayList<>();
            while (rsAuctions.next()) {
                auctionsList.add(new Object[]{rsAuctions.getInt("auction_id"), 
                                              rsAuctions.getInt("item_id"), 
                                              rsAuctions.getDouble("start_price")});
            }
            stmtGet.close();
            
            // 2. Tìm người đặt giá cao thứ hai (loại bỏ user chuẩn bị xóa) và cập nhật lại
            String sqlGetNextBid = "SELECT bidder_id, amount FROM bid_transactions " +
                                   "WHERE auction_id = ? AND bidder_id != ? " +
                                   "ORDER BY amount DESC, bid_time ASC LIMIT 1";
            PreparedStatement stmtNextBid = conn.prepareStatement(sqlGetNextBid);
            
            String sqlUpdateAuction = "UPDATE auctions SET highest_bidder_id = ? WHERE id = ?";
            String sqlUpdateAuctionNull = "UPDATE auctions SET highest_bidder_id = NULL WHERE id = ?";
            String sqlUpdateItem = "UPDATE items SET cur_highest = ? WHERE id = ?";
            
            PreparedStatement stmtUpdateAuction = conn.prepareStatement(sqlUpdateAuction);
            PreparedStatement stmtUpdateAuctionNull = conn.prepareStatement(sqlUpdateAuctionNull);
            PreparedStatement stmtUpdateItem = conn.prepareStatement(sqlUpdateItem);

            for (Object[] data : auctionsList) {
                int auctionId = (int) data[0];
                int itemId = (int) data[1];
                double startPrice = (double) data[2];
                
                stmtNextBid.setInt(1, auctionId);
                stmtNextBid.setInt(2, userId);
                ResultSet rsNext = stmtNextBid.executeQuery();
                
                if (rsNext.next()) {
                    // Nếu có người đứng thứ hai, thay thế top 1 bằng người này
                    int nextBidder = rsNext.getInt("bidder_id");
                    double nextAmount = rsNext.getDouble("amount");
                    
                    stmtUpdateAuction.setInt(1, nextBidder);
                    stmtUpdateAuction.setInt(2, auctionId);
                    stmtUpdateAuction.executeUpdate();
                    
                    stmtUpdateItem.setDouble(1, nextAmount);
                    stmtUpdateItem.setInt(2, itemId);
                    stmtUpdateItem.executeUpdate();
                } else {
                    // Nếu không còn ai đấu giá, reset về giá gốc và null bidder
                    stmtUpdateAuctionNull.setInt(1, auctionId);
                    stmtUpdateAuctionNull.executeUpdate();
                    
                    stmtUpdateItem.setDouble(1, startPrice);
                    stmtUpdateItem.setInt(2, itemId);
                    stmtUpdateItem.executeUpdate();
                }
                rsNext.close();
            }
            stmtNextBid.close();
            stmtUpdateAuction.close();
            stmtUpdateAuctionNull.close();
            stmtUpdateItem.close();

            // 3. Tiến hành xóa tài khoản khỏi CSDL
            String sql = "DELETE FROM users WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            int rowsAffected = stmt.executeUpdate();
            stmt.close();
            
            conn.commit();
            return rowsAffected > 0;
            
        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) { ex.printStackTrace(); }
            }
        }
    }
}
