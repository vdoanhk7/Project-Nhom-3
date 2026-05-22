package com.nhom3.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nhom3.server.db.DbConnection;
import com.nhom3.shared.model.auction.Auction;
import com.nhom3.shared.model.auction.BidTransaction;
import com.nhom3.shared.model.auction.StatusOfAuction;
import com.nhom3.shared.model.item.Item;
import com.nhom3.shared.model.user.Bidder;
import com.nhom3.shared.model.user.UserInfo;
import com.nhom3.shared.network.payload.AutoBidPayload;

public class AuctionDAOImpl implements AuctionDAO {
    private static final Logger log = LoggerFactory.getLogger(AuctionDAOImpl.class);
    private static final int DASHBOARD_TOP_LIMIT = 5;
    private static final int PAYMENT_GRACE_DAYS = 2;
    private static final int LATE_PENALTY_PER_DAY = 15;
    private static final int MAX_TRANSACTION_PENALTY = 60;
    private static final int PAYMENT_REWARD = 5;
    private static final String AUCTION_REPUTATION_PENALTY_COLUMN = "reputation_penalty";
    private static final String SELLER_RATINGS_TABLE = "seller_ratings";

    @Override
    public boolean updateHighestBid(int auctionId, int bidderId, double newAmount) {
        String sql = "UPDATE items i " +
                "JOIN auctions a ON i.id = a.item_id " +
                "JOIN users u ON u.id = ? " +
                "SET i.cur_highest = ? " +
                "WHERE a.id = ? " +
                "AND ((a.highest_bidder_id IS NULL AND ? >= i.cur_highest) " +
                "OR (a.highest_bidder_id IS NOT NULL AND ? >= i.cur_highest + a.bid_step)) " +
                "AND a.status = 'RUNNING' " +
                "AND u.role = 'BIDDER' " +
                "AND u.reputation_score > 0";

        String sqlAuction = "UPDATE auctions SET highest_bidder_id = ? WHERE id = ?";

        try (Connection conn = DbConnection.getConnection()) {
            UserDAOImpl.ensureReputationColumn(conn);
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement stmt1 = conn.prepareStatement(sql)) {
                    stmt1.setInt(1, bidderId);
                    stmt1.setDouble(2, newAmount);
                    stmt1.setInt(3, auctionId);
                    stmt1.setDouble(4, newAmount);
                    stmt1.setDouble(5, newAmount);

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
                conn.rollback();
                e.printStackTrace();
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean saveBidTransaction(BidTransaction bid, int auctionId) {
        // 1. Sửa lại SQL: Đổi NOW() thành dấu chấm hỏi (?)
        String sql = "INSERT INTO bid_transactions (auction_id, bidder_id, amount, bid_time, note) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DbConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, auctionId);
            stmt.setInt(2, bid.getBidder().getId());
            stmt.setDouble(3, bid.getAmount());

            // 2. Truyền giờ chuẩn Việt Nam của Java xuống (Không sợ AWS đổi giờ nữa)
            stmt.setTimestamp(4, java.sql.Timestamp.valueOf(bid.getBidTime()));

            stmt.setString(5, bid.getNote());

            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("[LỖI SQL] Không thể lưu lịch sử:");
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public List<Integer> closeExpiredAuctions(java.sql.Timestamp currentTime) {
        // Thay vì dùng NOW(), ta dùng dấu ? để truyền giờ Java vào
        List<Integer> changedAuctionIds = new ArrayList<>();
        String selectSql = "SELECT id FROM auctions WHERE status = 'RUNNING' AND end_time <= ?";
        String updateSql = "UPDATE auctions SET status = 'FINISHED' WHERE status = 'RUNNING' AND end_time <= ?";

        try (Connection conn = DbConnection.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
                stmt.setTimestamp(1, currentTime);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        changedAuctionIds.add(rs.getInt("id"));
                    }
                }
            }
            if (changedAuctionIds.isEmpty()) {
                return changedAuctionIds;
            }
            try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                stmt.setTimestamp(1, currentTime); // Đẩy giờ Java xuống Database
                int rowsUpdated = stmt.executeUpdate();
                if (rowsUpdated > 0) {
                    System.out.println("[Hệ thống] Đã tự động ĐÓNG " + rowsUpdated + " phiên đấu giá hết hạn!");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            changedAuctionIds.clear();
        }
        return changedAuctionIds;
    }

    @Override
    public Map<Integer, Integer> applyOverduePaymentPenalties(Timestamp currentTime) {
        Map<Integer, Integer> changedReputations = new HashMap<>();
        String sql = "SELECT id, highest_bidder_id, end_time, reputation_penalty "
                + "FROM auctions "
                + "WHERE status = 'FINISHED' "
                + "AND highest_bidder_id IS NOT NULL";

        try (Connection conn = DbConnection.getConnection()) {
            UserDAOImpl.ensureReputationColumn(conn);
            ensureAuctionReputationPenaltyColumn(conn);

            List<PenaltyCandidate> candidates = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                    ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    candidates.add(new PenaltyCandidate(
                            rs.getInt("id"),
                            rs.getInt("highest_bidder_id"),
                            rs.getTimestamp("end_time").toLocalDateTime(),
                            rs.getInt(AUCTION_REPUTATION_PENALTY_COLUMN)));
                }
            }

            LocalDateTime now = currentTime.toLocalDateTime();
            for (PenaltyCandidate candidate : candidates) {
                int targetPenalty = calculateLatePaymentPenalty(candidate.endTime(), now);
                if (targetPenalty <= candidate.currentPenalty()) {
                    continue;
                }

                conn.setAutoCommit(false);
                try {
                    applyReputationPenalty(
                            conn,
                            candidate.auctionId(),
                            candidate.bidderId(),
                            candidate.currentPenalty(),
                            targetPenalty,
                            targetPenalty >= MAX_TRANSACTION_PENALTY);
                    conn.commit();
                    changedReputations.put(candidate.bidderId(), getBidderReputation(candidate.bidderId()));
                } catch (Exception e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return changedReputations;
    }

    @Override
    public boolean createAuction(Auction auction) {
        String sql = "INSERT INTO auctions (item_id, start_time, end_time, bid_step, status) VALUES (?, ?, ?, ?, ?)";
        String resetPriceSql = "UPDATE items SET cur_highest = start_price WHERE id = ?";

        try (Connection conn = DbConnection.getConnection()) {
            ensureAuctionReputationPenaltyColumn(conn);
            conn.setAutoCommit(false);

            try (PreparedStatement resetStmt = conn.prepareStatement(resetPriceSql)) {
                resetStmt.setInt(1, auction.getItem().getId());
                resetStmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, auction.getItem().getId());
            stmt.setTimestamp(2, Timestamp.valueOf(auction.getStartTime()));
            stmt.setTimestamp(3, Timestamp.valueOf(auction.getEndTime()));
            stmt.setDouble(4, auction.getBidStep());
            stmt.setString(5, auction.getStatus().name());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                // Lấy ID tự tăng từ Database gán ngược lại cho đối tượng trên RAM
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        auction.setId(generatedKeys.getInt(1));
                    }
                }
                conn.commit();
                return true;
            }
            conn.rollback();
            }
        } catch (Exception e) {
            log.error("Lỗi SQL khi insert phiên đấu giá mới cho Item ID: {}", auction.getItem().getId(), e);
        }
        return false;
    }

    @Override
    public Map<Integer, String> getAuctionStatusBySeller(int sellerId) {
        Map<Integer, String> statusMap = new HashMap<>();
        String sql = "SELECT a.item_id, " +
                "CASE " +
                "   WHEN a.status IN ('PAID', 'CANCELLED', 'DEAL_CANCELLED') THEN a.status " +
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
        String sql = "SELECT a.*, i.id AS item_id, i.name AS item_name, i.description, i.item_type, "
                + "i.start_price, i.cur_highest, i.seller_id, u.full_name AS seller_name, "
                + "COALESCE(sr.seller_rating_avg, 0) AS seller_rating_avg, "
                + "COALESCE(sr.seller_rating_count, 0) AS seller_rating_count "
                + "FROM auctions a "
                + "JOIN items i ON a.item_id = i.id "
                + "JOIN users u ON i.seller_id = u.id "
                + "LEFT JOIN ("
                + "    SELECT seller_id, AVG(stars) AS seller_rating_avg, COUNT(*) AS seller_rating_count "
                + "    FROM seller_ratings GROUP BY seller_id"
                + ") sr ON sr.seller_id = i.seller_id "
                + "WHERE a.item_id = ? ORDER BY a.id DESC LIMIT 1";

        try (Connection conn = DbConnection.getConnection()) {
            ensureDescriptionColumn(conn);
            ensureSellerRatingsTable(conn);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, itemId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int id = rs.getInt("id");
                com.nhom3.shared.model.item.ItemType type =
                        com.nhom3.shared.model.item.ItemType.valueOf(rs.getString("item_type"));
                Item item = type.createItem(
                        rs.getInt("item_id"),
                        rs.getString("item_name"),
                        rs.getDouble("start_price"));
                applyDescription(item, rs);
                item.setCurHighest(rs.getDouble("cur_highest"));
                applySellerInfo(item, rs);
                java.time.LocalDateTime start = rs.getTimestamp("start_time").toLocalDateTime();
                java.time.LocalDateTime end = rs.getTimestamp("end_time").toLocalDateTime();
                // Khởi tạo Auction (Truyền null cho thuộc tính Item để tránh query vòng lặp, vì
                // bên giao diện ta chỉ cần lấy thời gian)
                Auction auction = new Auction(id, item, start, end);
                auction.setBidStep(readBidStep(rs));
                auction.setStatus(StatusOfAuction.valueOf(rs.getString("status")));
                int highestBidderId = rs.getInt("highest_bidder_id");
                if (!rs.wasNull()) {
                    auction.setHighestBidder(new Bidder(highestBidderId, null, null));
                }
                return auction;
            }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean confirmPayment(int auctionId, int sellerId) {
        String selectSql = "SELECT highest_bidder_id, end_time, reputation_penalty FROM auctions " +
                "WHERE id = ? " +
                "AND highest_bidder_id IS NOT NULL " +
                "AND (status = 'FINISHED' OR (status = 'RUNNING' AND end_time <= CURRENT_TIMESTAMP)) " +
                "AND EXISTS (SELECT 1 FROM items WHERE items.id = auctions.item_id AND seller_id = ?)";
        String updateAuctionSql = "UPDATE auctions SET status = 'PAID' WHERE id = ?";
        String rewardSql = "UPDATE users SET reputation_score = "
                + "CASE WHEN reputation_score + ? > 100 THEN 100 ELSE reputation_score + ? END "
                + "WHERE id = ?";

        try (Connection conn = DbConnection.getConnection()) {
            UserDAOImpl.ensureReputationColumn(conn);
            ensureAuctionReputationPenaltyColumn(conn);
            conn.setAutoCommit(false);

            int bidderId;
            int currentPenalty;
            LocalDateTime endTime;
            try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
                stmt.setInt(1, auctionId);
                stmt.setInt(2, sellerId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return false;
                    }
                    bidderId = rs.getInt("highest_bidder_id");
                    endTime = rs.getTimestamp("end_time").toLocalDateTime();
                    currentPenalty = rs.getInt(AUCTION_REPUTATION_PENALTY_COLUMN);
                }
            }

            int targetPenalty = calculateLatePaymentPenalty(endTime, LocalDateTime.now());
            if (targetPenalty >= MAX_TRANSACTION_PENALTY) {
                applyReputationPenalty(
                        conn, auctionId, bidderId, currentPenalty, MAX_TRANSACTION_PENALTY, true);
                conn.commit();
                return false;
            }
            if (targetPenalty > currentPenalty) {
                applyReputationPenalty(conn, auctionId, bidderId, currentPenalty, targetPenalty, false);
            }

            try (PreparedStatement stmt = conn.prepareStatement(updateAuctionSql)) {
                stmt.setInt(1, auctionId);
                if (stmt.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }
            }

            try (PreparedStatement stmt = conn.prepareStatement(rewardSql)) {
                stmt.setInt(1, PAYMENT_REWARD);
                stmt.setInt(2, PAYMENT_REWARD);
                stmt.setInt(3, bidderId);
                stmt.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean rateSeller(int auctionId, int buyerId, int stars) {
        if (auctionId <= 0 || buyerId <= 0 || stars < 0 || stars > 5) {
            return false;
        }

        String sql = "INSERT INTO seller_ratings (auction_id, buyer_id, seller_id, stars) "
                + "SELECT a.id, ?, i.seller_id, ? "
                + "FROM auctions a "
                + "JOIN items i ON a.item_id = i.id "
                + "WHERE a.id = ? "
                + "AND a.status = 'PAID' "
                + "AND a.highest_bidder_id = ? "
                + "AND i.seller_id <> ?";

        try (Connection conn = DbConnection.getConnection()) {
            ensureSellerRatingsTable(conn);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, buyerId);
                stmt.setInt(2, stars);
                stmt.setInt(3, auctionId);
                stmt.setInt(4, buyerId);
                stmt.setInt(5, buyerId);
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062 || "23505".equals(e.getSQLState())) {
                return false;
            }
            log.warn("Không thể lưu đánh giá seller cho auction {} từ buyer {}: {}",
                    auctionId, buyerId, e.getMessage());
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public int getBidderReputation(int bidderId) {
        String sql = "SELECT reputation_score FROM users WHERE id = ? AND role = 'BIDDER'";
        try (Connection conn = DbConnection.getConnection()) {
            UserDAOImpl.ensureReputationColumn(conn);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, bidderId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("reputation_score");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public String getBidderDisplayName(int bidderId) {
        String sql = "SELECT full_name FROM users WHERE id = ?";
        try (Connection conn = DbConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, bidderId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("full_name");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean cancelTransactionByBidder(int auctionId, int bidderId) {
        String sql = "SELECT reputation_penalty FROM auctions "
                + "WHERE id = ? "
                + "AND highest_bidder_id = ? "
                + "AND (status = 'FINISHED' OR (status = 'RUNNING' AND end_time <= CURRENT_TIMESTAMP))";

        try (Connection conn = DbConnection.getConnection()) {
            UserDAOImpl.ensureReputationColumn(conn);
            ensureAuctionReputationPenaltyColumn(conn);
            conn.setAutoCommit(false);

            int currentPenalty;
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, auctionId);
                stmt.setInt(2, bidderId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return false;
                    }
                    currentPenalty = rs.getInt(AUCTION_REPUTATION_PENALTY_COLUMN);
                }
            }

            applyReputationPenalty(conn, auctionId, bidderId, currentPenalty, MAX_TRANSACTION_PENALTY, true);
            conn.commit();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean cancelLatestUnpaidAuctionForRelist(int itemId, int sellerId) {
        String sql = "SELECT a.id, a.end_time, a.highest_bidder_id, a.reputation_penalty "
                + "FROM auctions a "
                + "JOIN items i ON a.item_id = i.id "
                + "WHERE a.item_id = ? "
                + "AND i.seller_id = ? "
                + "AND a.highest_bidder_id IS NOT NULL "
                + "AND (a.status = 'FINISHED' OR (a.status = 'RUNNING' AND a.end_time <= CURRENT_TIMESTAMP)) "
                + "ORDER BY a.id DESC LIMIT 1";

        try (Connection conn = DbConnection.getConnection()) {
            UserDAOImpl.ensureReputationColumn(conn);
            ensureAuctionReputationPenaltyColumn(conn);
            conn.setAutoCommit(false);

            int auctionId;
            int bidderId;
            int currentPenalty;
            LocalDateTime endTime;
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, itemId);
                stmt.setInt(2, sellerId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return false;
                    }
                    auctionId = rs.getInt("id");
                    endTime = rs.getTimestamp("end_time").toLocalDateTime();
                    bidderId = rs.getInt("highest_bidder_id");
                    currentPenalty = rs.getInt(AUCTION_REPUTATION_PENALTY_COLUMN);
                }
            }

            if (LocalDateTime.now().isBefore(endTime.plusDays(PAYMENT_GRACE_DAYS))) {
                conn.rollback();
                return false;
            }

            applyReputationPenalty(conn, auctionId, bidderId, currentPenalty, MAX_TRANSACTION_PENALTY, true);
            conn.commit();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean endAuction(int auctionId) {
        String sql = "UPDATE auctions SET status = 'FINISHED' WHERE id = ? AND status IN ('OPEN', 'RUNNING')";
        try (Connection conn = DbConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, auctionId);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean startAuction(int auctionId) {
        String sql = "UPDATE auctions SET status = 'RUNNING' WHERE id = ? AND status = 'OPEN'";
        try (Connection conn = DbConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, auctionId);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public List<Auction> getActiveAuctions() {
        List<Auction> list = new ArrayList<>();
        // Lấy thêm tên người bán 
        String sql = "SELECT a.*, i.seller_id, i.name, i.description, i.item_type, i.start_price, i.cur_highest, "
                + "u.full_name AS seller_name, "
                + "COALESCE(sr.seller_rating_avg, 0) AS seller_rating_avg, "
                + "COALESCE(sr.seller_rating_count, 0) AS seller_rating_count " +
            "FROM auctions a " +
            "JOIN items i ON a.item_id = i.id " +
            "JOIN users u ON i.seller_id = u.id " +
            "LEFT JOIN (" +
            "    SELECT seller_id, AVG(stars) AS seller_rating_avg, COUNT(*) AS seller_rating_count " +
            "    FROM seller_ratings GROUP BY seller_id" +
            ") sr ON sr.seller_id = i.seller_id " +
            "WHERE a.end_time > ? AND a.status IN ('OPEN', 'RUNNING') " +
            "ORDER BY a.end_time ASC";

        try (Connection conn = DbConnection.getConnection()) {
            ensureDescriptionColumn(conn);
            ensureSellerRatingsTable(conn);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, java.sql.Timestamp.valueOf(LocalDateTime.now()));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    com.nhom3.shared.model.item.ItemType type =
                            com.nhom3.shared.model.item.ItemType.valueOf(rs.getString("item_type"));
                    Item item = type.createItem(
                            rs.getInt("item_id"),
                            rs.getString("name"),
                            rs.getDouble("start_price"));
                    applyDescription(item, rs);
                    item.setCurHighest(rs.getDouble("cur_highest"));
                    applySellerInfo(item, rs);
                    int id = rs.getInt("id");
                    java.time.LocalDateTime start = rs.getTimestamp("start_time").toLocalDateTime();
                    java.time.LocalDateTime end = rs.getTimestamp("end_time").toLocalDateTime();
                    Auction auction = new Auction(id, item, start, end);
                    auction.setBidStep(readBidStep(rs));

                    java.time.LocalDateTime now = java.time.LocalDateTime.now();
                    if (now.isBefore(start)) {
                        auction.setStatus(StatusOfAuction.OPEN);
                    } else {
                        auction.setStatus(StatusOfAuction.RUNNING);
                    }
                    list.add(auction);
                }
            }
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi tải danh sách Chợ Đấu Giá: ");
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public List<Auction> getAllAuctions() {
        List<Auction> list = new ArrayList<>();
        String sql = "SELECT a.*, i.seller_id, i.name, i.description, i.item_type, i.start_price, i.cur_highest, "
                + "u.full_name AS seller_name, "
                + "COALESCE(sr.seller_rating_avg, 0) AS seller_rating_avg, "
                + "COALESCE(sr.seller_rating_count, 0) AS seller_rating_count " +
                "FROM auctions a " +
                "JOIN items i ON a.item_id = i.id " +
                "JOIN users u ON i.seller_id = u.id " +
                "LEFT JOIN (" +
                "    SELECT seller_id, AVG(stars) AS seller_rating_avg, COUNT(*) AS seller_rating_count " +
                "    FROM seller_ratings GROUP BY seller_id" +
                ") sr ON sr.seller_id = i.seller_id " +
                "ORDER BY a.id DESC";

        try (Connection conn = DbConnection.getConnection()) {
            ensureDescriptionColumn(conn);
            ensureSellerRatingsTable(conn);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    com.nhom3.shared.model.item.ItemType type =
                            com.nhom3.shared.model.item.ItemType.valueOf(rs.getString("item_type"));
                    Item item = type.createItem(
                            rs.getInt("item_id"),
                            rs.getString("name"),
                            rs.getDouble("start_price"));
                    applyDescription(item, rs);
                    item.setCurHighest(rs.getDouble("cur_highest"));
                    applySellerInfo(item, rs);
                    int id = rs.getInt("id");
                    java.time.LocalDateTime start = rs.getTimestamp("start_time").toLocalDateTime();
                    java.time.LocalDateTime end = rs.getTimestamp("end_time").toLocalDateTime();
                    Auction auction = new Auction(id, item, start, end);
                    auction.setBidStep(readBidStep(rs));

                    java.time.LocalDateTime now = java.time.LocalDateTime.now();
                    String dbStatus = rs.getString("status");
                    if ("PAID".equals(dbStatus)
                            || "CANCELLED".equals(dbStatus)
                            || "DEAL_CANCELLED".equals(dbStatus)
                            || "FINISHED".equals(dbStatus)) {
                        auction.setStatus(StatusOfAuction.valueOf(dbStatus));
                    } else if (now.isBefore(start)) {
                        auction.setStatus(StatusOfAuction.OPEN);
                    } else if (now.isAfter(end) || now.isEqual(end)) {
                        auction.setStatus(StatusOfAuction.FINISHED);
                    } else {
                        auction.setStatus(StatusOfAuction.RUNNING);
                    }
                    list.add(auction);
                }
            }
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi tải toàn bộ danh sách Đấu Giá: ");
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
                UserInfo userInfo = new UserInfo("", "", rs.getString("bidder_name"));
                Bidder bidder = new Bidder(rs.getInt("bidder_id"), userInfo, null);

                BidTransaction bid = new BidTransaction(
                        rs.getInt("id"),
                        bidder,
                        rs.getDouble("amount"),
                        rs.getTimestamp("bid_time").toLocalDateTime(), // rs sẽ đọc đúng tên cột bid_time
                        rs.getString("note"));
                list.add(bid);
            }
        } catch (Exception e) {
            System.err.println("❌ Lỗi SQL khi tải Lịch sử trả giá:");
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public List<Auction> getMyBidHistory(int bidderId) {
        List<Auction> list = new ArrayList<>();

        // 1. SỬ DỤNG GOM NHÓM (GROUP BY) TRONG SQL ĐỂ TỐI ƯU
        // Ta tạo một bảng ảo "my_bids" chỉ chứa mức giá CAO NHẤT (MAX) của user này cho
        // từng auction_id
        String sql = "SELECT a.id AS auction_id, a.highest_bidder_id, a.start_time, a.end_time, a.status, " +
                "a.bid_step, i.id AS item_id, i.seller_id, seller.full_name AS seller_name, " +
                "COALESCE(sr.seller_rating_avg, 0) AS seller_rating_avg, " +
                "COALESCE(sr.seller_rating_count, 0) AS seller_rating_count, " +
                "seller_rating.stars AS my_seller_rating, " +
                "i.name AS item_name, i.description, i.item_type, i.start_price, i.cur_highest, i.image, " +
                "my_bids.max_amount AS amount, my_bids.last_bid_time AS bid_time " +
                "FROM ( " +
                "    SELECT auction_id, MAX(amount) AS max_amount, MAX(bid_time) AS last_bid_time " +
                "    FROM bid_transactions " +
                "    WHERE bidder_id = ? " +
                "    GROUP BY auction_id " +
                ") my_bids " +
                "JOIN auctions a ON my_bids.auction_id = a.id " +
                "JOIN items i ON a.item_id = i.id " +
                "JOIN users seller ON i.seller_id = seller.id " +
                "LEFT JOIN ( " +
                "    SELECT seller_id, AVG(stars) AS seller_rating_avg, COUNT(*) AS seller_rating_count " +
                "    FROM seller_ratings GROUP BY seller_id " +
                ") sr ON sr.seller_id = i.seller_id " +
                "LEFT JOIN seller_ratings seller_rating "
                + "ON seller_rating.auction_id = a.id AND seller_rating.buyer_id = ? " +
                "ORDER BY my_bids.last_bid_time DESC";

        try (Connection conn = DbConnection.getConnection()) {
            ensureDescriptionColumn(conn);
            ensureSellerRatingsTable(conn);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, bidderId);
            stmt.setInt(2, bidderId);
            ResultSet rs = stmt.executeQuery();

            // Lấy giờ chuẩn của máy chủ Java
            LocalDateTime javaNow = LocalDateTime.now();

            while (rs.next()) {
                com.nhom3.shared.model.item.ItemType type =
                        com.nhom3.shared.model.item.ItemType.valueOf(rs.getString("item_type"));
                Item item = type.createItem(
                        rs.getInt("item_id"),
                        rs.getString("item_name"),
                        rs.getDouble("start_price"));
                applyDescription(item, rs);
                item.setCurHighest(rs.getDouble("cur_highest"));
                item.setImageBase64(rs.getString("image"));
                applySellerInfo(item, rs);

                LocalDateTime start = rs.getTimestamp("start_time").toLocalDateTime();
                LocalDateTime end = rs.getTimestamp("end_time").toLocalDateTime();
                Auction auction = new Auction(rs.getInt("auction_id"), item, start, end);
                auction.setBidStep(readBidStep(rs));

                // TÍNH TOÁN LẠI TRẠNG THÁI BẰNG JAVA
                String dbStatus = rs.getString("status");
                if ("PAID".equals(dbStatus) || "CANCELLED".equals(dbStatus) || "DEAL_CANCELLED".equals(dbStatus)) {
                    auction.setStatus(StatusOfAuction.valueOf(dbStatus));
                } else if (javaNow.isAfter(end) || javaNow.isEqual(end)) {
                    auction.setStatus(StatusOfAuction.FINISHED);
                } else if (javaNow.isAfter(start) || javaNow.isEqual(start)) {
                    auction.setStatus(StatusOfAuction.RUNNING);
                } else {
                    auction.setStatus(StatusOfAuction.OPEN);
                }

                Bidder topBidder = new Bidder(rs.getInt("highest_bidder_id"), null, null);
                auction.setHighestBidder(topBidder);

                // Parse dữ liệu "Giá cao nhất" và "Thời gian cuối cùng" mà user này đặt
                BidTransaction myBid = new BidTransaction(0, null, rs.getDouble("amount"),
                        rs.getTimestamp("bid_time").toLocalDateTime(), "");
                auction.getBidHistory().add(myBid);
                int mySellerRating = rs.getInt("my_seller_rating");
                auction.setSellerRatingByCurrentBuyer(rs.wasNull() ? -1 : mySellerRating);

                list.add(auction);
            }
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

    @Override
    public boolean cancelAuction(int auctionId) {
        String sql = "UPDATE auctions SET status = 'CANCELLED' WHERE id = ? AND status IN ('OPEN', 'RUNNING')";
        String deleteAutoBidsSql = "DELETE FROM auto_bids WHERE auction_id = ?";

        try (Connection conn = DbConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, auctionId);
                int rowsUpdated = stmt.executeUpdate();
                if (rowsUpdated == 0) {
                    conn.rollback();
                    return false;
                }
            }
            try (PreparedStatement stmt = conn.prepareStatement(deleteAutoBidsSql)) {
                stmt.setInt(1, auctionId);
                stmt.executeUpdate();
            } catch (Exception e) {
                // Optional table may not exist in older local databases/tests.
            }
            conn.commit();
            return true;

        } catch (Exception e) {
            System.err.println("[DAO] Lỗi khi hủy phiên đấu giá: " + e.getMessage());
            return false;
        }
    }

    @Override
    public LocalDateTime getEndTime(int auctionId) {
        String sql = "SELECT end_time FROM auctions WHERE id = ?";
        try (Connection conn = DbConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, auctionId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getTimestamp("end_time").toLocalDateTime();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Integer> startScheduledAuctions(java.sql.Timestamp currentTime) {
        // Thay vì dùng NOW(), ta dùng dấu ? để truyền giờ Java vào
        List<Integer> changedAuctionIds = new ArrayList<>();
        String selectSql = "SELECT id FROM auctions WHERE status = 'OPEN' AND start_time <= ?";
        String updateSql = "UPDATE auctions SET status = 'RUNNING' WHERE status = 'OPEN' AND start_time <= ?";

        try (Connection conn = DbConnection.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
                stmt.setTimestamp(1, currentTime);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        changedAuctionIds.add(rs.getInt("id"));
                    }
                }
            }
            if (changedAuctionIds.isEmpty()) {
                return changedAuctionIds;
            }
            try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                stmt.setTimestamp(1, currentTime); // Đẩy giờ Java xuống Database
                int rowsUpdated = stmt.executeUpdate();
                if (rowsUpdated > 0) {
                    System.out.println("[Hệ thống] Đã tự động MỞ " + rowsUpdated + " phiên đấu giá đến giờ lên sàn!");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            changedAuctionIds.clear();
        }
        return changedAuctionIds;
    }

    @Override
    public boolean saveAutoBidConfig(com.nhom3.shared.network.payload.AutoBidPayload payload) {
        // Cập nhật lại cấu hình nếu người dùng đã đặt Auto-Bid trước đó, nếu chưa thì
        // thêm mới
        String sqlCheck = "SELECT id FROM auto_bids WHERE bidder_id = ? AND auction_id = ?";
        String sqlUpdate = "UPDATE auto_bids SET max_amount = ?, increment_amount = ? WHERE bidder_id = ? AND auction_id = ?";
        String sqlInsert = "INSERT INTO auto_bids (bidder_id, auction_id, max_amount, increment_amount) VALUES (?, ?, ?, ?)";
        String sqlValidate = "SELECT 1 FROM auctions a JOIN users u ON u.id = ? " +
                "WHERE a.id = ? AND a.status = 'RUNNING' " +
                "AND u.role = 'BIDDER' AND u.reputation_score > 0";

        try (Connection conn = DbConnection.getConnection()) {
            UserDAOImpl.ensureReputationColumn(conn);
            try (PreparedStatement validateStmt = conn.prepareStatement(sqlValidate)) {
                validateStmt.setInt(1, payload.getUserId());
                validateStmt.setInt(2, payload.getAuctionId());
                try (ResultSet rs = validateStmt.executeQuery()) {
                    if (!rs.next()) {
                        return false;
                    }
                }
            }

            boolean exists = false;
            try (PreparedStatement checkStmt = conn.prepareStatement(sqlCheck)) {
                checkStmt.setInt(1, payload.getUserId());
                checkStmt.setInt(2, payload.getAuctionId());
                ResultSet rs = checkStmt.executeQuery();
                exists = rs.next();
            }

            if (exists) {
                try (PreparedStatement updateStmt = conn.prepareStatement(sqlUpdate)) {
                    updateStmt.setDouble(1, payload.getMaxAmount());
                    updateStmt.setDouble(2, payload.getIncrement());
                    updateStmt.setInt(3, payload.getUserId());
                    updateStmt.setInt(4, payload.getAuctionId());
                    return updateStmt.executeUpdate() > 0;
                }
            } else {
                try (PreparedStatement insertStmt = conn.prepareStatement(sqlInsert)) {
                    insertStmt.setInt(1, payload.getUserId());
                    insertStmt.setInt(2, payload.getAuctionId());
                    insertStmt.setDouble(3, payload.getMaxAmount());
                    insertStmt.setDouble(4, payload.getIncrement());
                    return insertStmt.executeUpdate() > 0;
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Lỗi khi lưu cấu hình Auto-Bid:");
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public com.nhom3.shared.network.payload.DashboardResponsePayload getDashboardStats() {
        int totalUsers = 0;
        int activeItems = 0;
        double totalRevenue = 0.0;
        List<com.nhom3.shared.network.payload.DashboardResponsePayload.TopBidderDTO> topBidders = new ArrayList<>();
        List<com.nhom3.shared.network.payload.DashboardResponsePayload.TopItemDTO> topItems = new ArrayList<>();

        try (Connection conn = DbConnection.getConnection()) {
            // 1. Đếm người dùng
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM users");
                    ResultSet rs = stmt.executeQuery()) {
                if (rs.next())
                    totalUsers = rs.getInt(1);
            }
            // 2. Đếm sản phẩm đang mở/đang chạy
            try (PreparedStatement stmt = conn
                    .prepareStatement("SELECT COUNT(*) FROM auctions WHERE status IN ('OPEN', 'RUNNING')");
                    ResultSet rs = stmt.executeQuery()) {
                if (rs.next())
                    activeItems = rs.getInt(1);
            }
            // 3. Tính tổng doanh thu (Các đơn đã PAID)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT SUM(i.cur_highest) FROM auctions a JOIN items i ON a.item_id = i.id WHERE a.status = 'PAID'");
                    ResultSet rs = stmt.executeQuery()) {
                if (rs.next())
                    totalRevenue = rs.getDouble(1);
            }
            // 4. Lấy Top 5 Đại gia (Bidder mua nhiều tiền nhất)
            String sqlTopBidder = "SELECT u.full_name, SUM(i.cur_highest) AS total_spent FROM auctions a JOIN items i ON a.item_id = i.id JOIN users u ON a.highest_bidder_id = u.id WHERE a.status = 'PAID' GROUP BY u.id, u.full_name ORDER BY total_spent DESC LIMIT ?";
            try (PreparedStatement stmt = conn.prepareStatement(sqlTopBidder)) {
                stmt.setInt(1, DASHBOARD_TOP_LIMIT);
                try (ResultSet rs = stmt.executeQuery()) {
                    int rank = 1;
                    while (rs.next())
                        topBidders.add(
                                new com.nhom3.shared.network.payload.DashboardResponsePayload.TopBidderDTO(rank++,
                                        rs.getString("full_name"), rs.getDouble("total_spent")));
                }
            }
            // 5. Lấy Top 5 Sản phẩm đắt nhất đã thanh toán
            String sqlTopItem = "SELECT i.name, i.cur_highest FROM auctions a JOIN items i ON a.item_id = i.id WHERE a.status = 'PAID' ORDER BY i.cur_highest DESC LIMIT ?";
            try (PreparedStatement stmt = conn.prepareStatement(sqlTopItem)) {
                stmt.setInt(1, DASHBOARD_TOP_LIMIT);
                try (ResultSet rs = stmt.executeQuery()) {
                    int rank = 1;
                    while (rs.next())
                        topItems.add(new com.nhom3.shared.network.payload.DashboardResponsePayload.TopItemDTO(rank++,
                                rs.getString("name"), rs.getDouble("cur_highest")));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new com.nhom3.shared.network.payload.DashboardResponsePayload(totalUsers, activeItems, totalRevenue,
                topBidders, topItems);
    }

    @Override
    public Auction getAuctionById(int auctionId) {
        String sql = "SELECT a.*, i.id AS item_id, i.seller_id, i.name AS item_name, i.description, i.item_type, "
                + "i.start_price, i.cur_highest, i.image, u.full_name AS seller_name, "
                + "COALESCE(sr.seller_rating_avg, 0) AS seller_rating_avg, "
                + "COALESCE(sr.seller_rating_count, 0) AS seller_rating_count "
                + "FROM auctions a "
                + "JOIN items i ON a.item_id = i.id "
                + "JOIN users u ON i.seller_id = u.id "
                + "LEFT JOIN ("
                + "    SELECT seller_id, AVG(stars) AS seller_rating_avg, COUNT(*) AS seller_rating_count "
                + "    FROM seller_ratings GROUP BY seller_id"
                + ") sr ON sr.seller_id = i.seller_id "
                + "WHERE a.id = ?";
        try (Connection conn = DbConnection.getConnection()) {
            ensureDescriptionColumn(conn);
            ensureSellerRatingsTable(conn);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, auctionId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                com.nhom3.shared.model.item.ItemType type = com.nhom3.shared.model.item.ItemType.valueOf(rs.getString("item_type"));
                Item item = type.createItem(rs.getInt("item_id"), rs.getString("item_name"), rs.getDouble("start_price"));
                applyDescription(item, rs);
                item.setCurHighest(rs.getDouble("cur_highest"));
                item.setImageBase64(rs.getString("image"));
                applySellerInfo(item, rs);

                LocalDateTime start = rs.getTimestamp("start_time").toLocalDateTime();
                LocalDateTime end = rs.getTimestamp("end_time").toLocalDateTime();
                Auction auction = new Auction(auctionId, item, start, end);
                auction.setBidStep(readBidStep(rs));
                auction.setStatus(StatusOfAuction.valueOf(rs.getString("status")));
                int highestBidderId = rs.getInt("highest_bidder_id");
                if (!rs.wasNull()) {
                    auction.setHighestBidder(new Bidder(highestBidderId, null, null));
                }
                return auction;
            }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void applyDescription(Item item, ResultSet rs) {
        try {
            item.setDescription(rs.getString("description"));
        } catch (Exception e) {
            item.setDescription("");
        }
    }

    private void applySellerInfo(Item item, ResultSet rs) {
        try {
            item.setSellerId(rs.getInt("seller_id"));
        } catch (Exception e) {
            item.setSellerId(-1);
        }
        try {
            item.setSellerName(rs.getString("seller_name"));
        } catch (Exception e) {
            item.setSellerName("");
        }
        double average = 0;
        int count = 0;
        try {
            average = rs.getDouble("seller_rating_avg");
        } catch (Exception e) {
            average = 0;
        }
        try {
            count = rs.getInt("seller_rating_count");
        } catch (Exception e) {
            count = 0;
        }
        item.setSellerRatingSummary(average, count);
    }

    private void ensureDescriptionColumn(Connection conn) {
        try {
            if (hasColumn(conn, "items", "description") || hasColumn(conn, "ITEMS", "DESCRIPTION")) {
                return;
            }
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("ALTER TABLE items ADD COLUMN description TEXT NULL");
            }
        } catch (Exception e) {
            // Older local databases are migrated opportunistically; callers still handle SQL failures.
        }
    }

    private boolean hasColumn(Connection conn, String tableName, String columnName) throws Exception {
        try (ResultSet columns = conn.getMetaData().getColumns(null, null, tableName, columnName)) {
            return columns.next();
        }
    }

    private double readBidStep(ResultSet rs) {
        try {
            double bidStep = rs.getDouble("bid_step");
            if (!rs.wasNull() && bidStep > 0) {
                return bidStep;
            }
        } catch (Exception e) {
            // Older result sets/tests may not contain bid_step; keep existing behavior.
        }
        return Auction.DEFAULT_BID_STEP;
    }

    private int calculateLatePaymentPenalty(LocalDateTime endTime, LocalDateTime now) {
        LocalDateTime dueTime = endTime.plusDays(PAYMENT_GRACE_DAYS);
        if (!now.isAfter(dueTime)) {
            return 0;
        }

        long lateDays = ChronoUnit.DAYS.between(dueTime, now);
        if (lateDays <= 0) {
            return 0;
        }
        return Math.min(MAX_TRANSACTION_PENALTY, (int) lateDays * LATE_PENALTY_PER_DAY);
    }

    private void applyReputationPenalty(Connection conn, int auctionId, int bidderId,
            int currentPenalty, int targetPenalty, boolean cancelDeal) throws SQLException {
        int normalizedTargetPenalty = Math.min(MAX_TRANSACTION_PENALTY, Math.max(0, targetPenalty));
        int normalizedCurrentPenalty = Math.min(MAX_TRANSACTION_PENALTY, Math.max(0, currentPenalty));
        int delta = Math.max(0, normalizedTargetPenalty - normalizedCurrentPenalty);

        if (delta > 0) {
            String updateUserSql = "UPDATE users SET reputation_score = "
                    + "CASE WHEN reputation_score - ? < 0 THEN 0 ELSE reputation_score - ? END "
                    + "WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(updateUserSql)) {
                stmt.setInt(1, delta);
                stmt.setInt(2, delta);
                stmt.setInt(3, bidderId);
                stmt.executeUpdate();
            }
        }

        String updateAuctionSql = cancelDeal
                ? "UPDATE auctions SET reputation_penalty = ?, status = 'DEAL_CANCELLED' WHERE id = ?"
                : "UPDATE auctions SET reputation_penalty = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(updateAuctionSql)) {
            stmt.setInt(1, normalizedTargetPenalty);
            stmt.setInt(2, auctionId);
            stmt.executeUpdate();
        }
    }

    private void ensureAuctionReputationPenaltyColumn(Connection conn) {
        try {
            if (hasColumn(conn, "auctions", AUCTION_REPUTATION_PENALTY_COLUMN)
                    || hasColumn(conn, "AUCTIONS", AUCTION_REPUTATION_PENALTY_COLUMN.toUpperCase())) {
                return;
            }
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("ALTER TABLE auctions ADD COLUMN reputation_penalty INT NOT NULL DEFAULT 0");
            }
        } catch (Exception e) {
            // Older local databases are migrated opportunistically; callers still handle SQL failures.
        }
    }

    public static void ensureSellerRatingsTable(Connection conn) {
        String sql = "CREATE TABLE IF NOT EXISTS " + SELLER_RATINGS_TABLE + " ("
                + "id INT AUTO_INCREMENT PRIMARY KEY,"
                + "auction_id INT NOT NULL,"
                + "buyer_id INT NOT NULL,"
                + "seller_id INT NOT NULL,"
                + "stars INT NOT NULL,"
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                + "UNIQUE KEY unique_seller_rating_per_auction_buyer (auction_id, buyer_id),"
                + "FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE,"
                + "FOREIGN KEY (buyer_id) REFERENCES users(id) ON DELETE CASCADE,"
                + "FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE"
                + ")";
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (NullPointerException e) {
            // Unit tests may use minimal mocked connections without statements.
        } catch (SQLException e) {
            // Older local databases are migrated opportunistically; callers still handle SQL failures.
        }
    }

    private record PenaltyCandidate(
            int auctionId,
            int bidderId,
            LocalDateTime endTime,
            int currentPenalty) {
    }

    @Override
    public List<AutoBidPayload> getActiveAutoBids(int auctionId) {
        List<AutoBidPayload> list = new ArrayList<>();
        String sql = "SELECT ab.bidder_id, ab.max_amount, ab.increment_amount "
                + "FROM auto_bids ab "
                + "JOIN users u ON u.id = ab.bidder_id "
                + "WHERE ab.auction_id = ? "
                + "AND u.reputation_score > 0 "
                + "ORDER BY ab.id ASC";
        try (Connection conn = DbConnection.getConnection()) {
            UserDAOImpl.ensureReputationColumn(conn);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, auctionId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(new AutoBidPayload(
                        rs.getInt("bidder_id"),
                        auctionId,
                        rs.getDouble("max_amount"),
                        rs.getDouble("increment_amount")));
            }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public AutoBidPayload getUserAutoBid(int auctionId, int userId) {
        String sql = "SELECT max_amount, increment_amount FROM auto_bids WHERE auction_id = ? AND bidder_id = ?";
        try (Connection conn = DbConnection.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, auctionId);
            stmt.setInt(2, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new AutoBidPayload(userId, auctionId, rs.getDouble("max_amount"),
                        rs.getDouble("increment_amount"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean cancelAutoBid(int auctionId, int userId) {
        String sql = "DELETE FROM auto_bids WHERE auction_id = ? AND bidder_id = ?";
        try (Connection conn = DbConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, auctionId);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
