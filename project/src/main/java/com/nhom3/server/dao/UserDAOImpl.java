package com.nhom3.server.dao;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.mindrot.jbcrypt.BCrypt;

import com.nhom3.server.db.DbConnection;
import com.nhom3.server.exception.UserOperationException;
import com.nhom3.shared.model.user.Admin;
import com.nhom3.shared.model.user.Bidder;
import com.nhom3.shared.model.user.Role;
import com.nhom3.shared.model.user.Seller;
import com.nhom3.shared.model.user.User;
import com.nhom3.shared.model.user.UserContact;
import com.nhom3.shared.model.user.UserInfo;

public class UserDAOImpl implements UserDAO {
    private static final String PROFILE_IMAGE_COLUMN = "profile_image";
    private static final String REPUTATION_COLUMN = "reputation_score";
    private static final int MYSQL_DUPLICATE_COLUMN_ERROR = 1060;

    private User mapUser(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String role = rs.getString("role");
        UserInfo info = new UserInfo(
                rs.getString("username"),
                rs.getString("password"),
                rs.getString("full_name"));
        info.setProfileImageBase64(readOptionalString(rs, PROFILE_IMAGE_COLUMN));
        UserContact contact = new UserContact(
                rs.getString("email"),
                rs.getString("phone"));
        User user;
        if (role.equals("BIDDER")) {
            user = new Bidder(id, info, contact);
        } else if (role.equals("SELLER")) {
            user = new Seller(id, info, contact);
        } else if (role.equals("ADMIN")) {
            user = new Admin(id, info, contact);
        } else {
            return null;
        }
        user.setReputationScore(readOptionalInt(
                rs, REPUTATION_COLUMN, User.DEFAULT_REPUTATION_SCORE));
        return user;
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
                    User user = mapUser(rs);
                    attachSellerRatingSummary(conn, user);
                    return user;
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
        boolean hasNewProfileImage = user.getUserInfo().getProfileImageBase64() != null;
        String sql = hasNewProfileImage
                ? "UPDATE users SET full_name = ?, email = ?, phone = ?, profile_image = ? WHERE id = ?"
                : "UPDATE users SET full_name = ?, email = ?, phone = ? WHERE id = ?";
        try (Connection conn = DbConnection.getConnection()) {
            if (hasNewProfileImage) {
                ensureProfileImageColumn(conn);
            }
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, user.getUserInfo().getName());
                stmt.setString(2, user.getUserContact().getEmail());
                stmt.setString(3, user.getUserContact().getPhoneNumber());
                if (hasNewProfileImage) {
                    stmt.setString(4, user.getUserInfo().getProfileImageBase64());
                    stmt.setInt(5, user.getId());
                } else {
                    stmt.setInt(4, user.getId());
                }
                return stmt.executeUpdate() > 0;
            }
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
    public User getById(int userId) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = DbConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    User user = mapUser(rs);
                    attachSellerRatingSummary(conn, user);
                    return user;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
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
            attachSellerRatingSummaries(conn, users);
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
            if (hasLockedWinningAuctions(conn, userId)) {
                throw new UserOperationException(
                        "USER_DELETE_LOCKED_WINNER",
                        "Không thể xóa tài khoản này vì đang là người thắng của phiên đã kết thúc hoặc đã thanh toán.");
            }
            deleteAutoBidsForSellerAuctions(conn, userId);
            deleteAutoBidsForUser(conn, userId);

            // 1. Tìm tất cả các phiên đấu giá mà user này đang giữ top 1 (highest_bidder_id)
            String sqlGetAuctions = "SELECT a.id AS auction_id, a.item_id, i.start_price " +
                                    "FROM auctions a JOIN items i ON a.item_id = i.id " +
                                    "WHERE a.highest_bidder_id = ? AND a.status IN ('OPEN', 'RUNNING')";
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
            
        } catch (UserOperationException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            throw e;
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

    @Override
    public boolean userExists(int userId) {
        String sql = "SELECT 1 FROM users WHERE id = ? LIMIT 1";
        try (Connection conn = DbConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public List<Integer> getSellerAuctionIds(int sellerId) {
        List<Integer> auctionIds = new ArrayList<>();
        String sql = "SELECT a.id FROM auctions a JOIN items i ON a.item_id = i.id WHERE i.seller_id = ?";
        try (Connection conn = DbConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, sellerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    auctionIds.add(rs.getInt("id"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return auctionIds;
    }

    private boolean hasLockedWinningAuctions(Connection conn, int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM auctions " +
                "WHERE highest_bidder_id = ? " +
                "AND (status IN ('FINISHED', 'PAID') " +
                "OR (status = 'RUNNING' AND end_time <= CURRENT_TIMESTAMP))";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private void deleteAutoBidsForUser(Connection conn, int userId) {
        String sql = "DELETE FROM auto_bids WHERE bidder_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            // Optional table may not exist in older local databases/tests.
        }
    }

    private void deleteAutoBidsForSellerAuctions(Connection conn, int sellerId) {
        String sql = "DELETE FROM auto_bids WHERE auction_id IN (" +
                "SELECT a.id FROM auctions a JOIN items i ON a.item_id = i.id WHERE i.seller_id = ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, sellerId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            // Optional table may not exist in older local databases/tests.
        }
    }

    private String readOptionalString(ResultSet rs, String columnName) throws SQLException {
        if (!hasColumn(rs, columnName)) {
            return null;
        }
        return rs.getString(columnName);
    }

    private int readOptionalInt(ResultSet rs, String columnName, int defaultValue) throws SQLException {
        if (!hasColumn(rs, columnName)) {
            return defaultValue;
        }
        int value = rs.getInt(columnName);
        return rs.wasNull() ? defaultValue : value;
    }

    private boolean hasColumn(ResultSet rs, String columnName) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        if (metaData == null) {
            return false;
        }
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            if (columnName.equalsIgnoreCase(metaData.getColumnName(i))) {
                return true;
            }
        }
        return false;
    }

    private void ensureProfileImageColumn(Connection conn) throws SQLException {
        if (hasProfileImageColumn(conn)) {
            return;
        }

        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("ALTER TABLE users ADD COLUMN profile_image MEDIUMTEXT NULL");
        } catch (SQLException e) {
            if (e.getErrorCode() != MYSQL_DUPLICATE_COLUMN_ERROR) {
                throw e;
            }
        }
    }

    private boolean hasProfileImageColumn(Connection conn) throws SQLException {
        DatabaseMetaData metaData = conn.getMetaData();
        try (ResultSet columns = metaData.getColumns(
                conn.getCatalog(), null, "users", PROFILE_IMAGE_COLUMN)) {
            return columns.next();
        }
    }

    public static void ensureReputationColumn(Connection conn) throws SQLException {
        try {
            if (hasUserColumn(conn, REPUTATION_COLUMN)) {
                return;
            }

            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("ALTER TABLE users ADD COLUMN reputation_score INT NOT NULL DEFAULT 100");
            } catch (SQLException e) {
                if (e.getErrorCode() != MYSQL_DUPLICATE_COLUMN_ERROR) {
                    throw e;
                }
            }
        } catch (NullPointerException e) {
            // Unit tests may use minimal mocked connections without metadata.
        }
    }

    private static boolean hasUserColumn(Connection conn, String columnName) throws SQLException {
        DatabaseMetaData metaData = conn.getMetaData();
        if (metaData == null) {
            return true;
        }
        try (ResultSet columns = metaData.getColumns(conn.getCatalog(), null, "users", columnName)) {
            if (columns.next()) {
                return true;
            }
        }
        try (ResultSet columns = metaData.getColumns(conn.getCatalog(), null, "USERS", columnName.toUpperCase())) {
            return columns.next();
        }
    }

    private void attachSellerRatingSummary(Connection conn, User user) {
        if (user == null || user.getRole() != Role.SELLER) {
            return;
        }

        String sql = "SELECT AVG(stars) AS avg_stars, COUNT(*) AS rating_count "
                + "FROM seller_ratings WHERE seller_id = ?";
        try {
            AuctionDAOImpl.ensureSellerRatingsTable(conn);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, user.getId());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        double average = rs.getDouble("avg_stars");
                        if (rs.wasNull()) {
                            average = 0;
                        }
                        user.setSellerRatingSummary(average, rs.getInt("rating_count"));
                    }
                }
            }
        } catch (Exception e) {
            user.setSellerRatingSummary(0, 0);
        }
    }

    private void attachSellerRatingSummaries(Connection conn, List<User> users) {
        if (users == null || users.isEmpty()) {
            return;
        }

        String sql = "SELECT seller_id, AVG(stars) AS avg_stars, COUNT(*) AS rating_count "
                + "FROM seller_ratings GROUP BY seller_id";
        try {
            AuctionDAOImpl.ensureSellerRatingsTable(conn);
            java.util.Map<Integer, double[]> ratingMap = new java.util.HashMap<>();
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                    ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ratingMap.put(rs.getInt("seller_id"),
                            new double[] { rs.getDouble("avg_stars"), rs.getInt("rating_count") });
                }
            }

            for (User user : users) {
                if (user == null || user.getRole() != Role.SELLER) {
                    continue;
                }
                double[] rating = ratingMap.get(user.getId());
                if (rating == null) {
                    user.setSellerRatingSummary(0, 0);
                } else {
                    user.setSellerRatingSummary(rating[0], (int) rating[1]);
                }
            }
        } catch (Exception e) {
            for (User user : users) {
                if (user != null && user.getRole() == Role.SELLER) {
                    user.setSellerRatingSummary(0, 0);
                }
            }
        }
    }
}
