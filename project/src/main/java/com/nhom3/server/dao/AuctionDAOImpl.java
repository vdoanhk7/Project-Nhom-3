package com.nhom3.server.dao;

import com.nhom3.server.db.DbConnection;

import com.nhom3.shared.model.auction.Auction;
import com.nhom3.shared.model.auction.BidTransaction;
import com.nhom3.shared.model.auction.StatusOfAuction;
import com.nhom3.shared.model.user.Bidder;
import com.nhom3.shared.model.user.UserInfo;
import com.nhom3.shared.network.payload.AutoBidPayload;
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
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class AuctionDAOImpl implements AuctionDAO {
    private static final Logger log = LoggerFactory.getLogger(AuctionDAOImpl.class);

    @Override
    public boolean updateHighestBid(int auctionId, int bidderId, double newAmount) {
        String sql = "UPDATE items i " +
                "JOIN auctions a ON i.id = a.item_id " +
                "SET i.cur_highest = ? " +
                "WHERE a.id = ? " +
                "AND ? >= i.cur_highest + a.bid_step " +
                "AND a.status = 'RUNNING'";

        String sqlAuction = "UPDATE auctions SET highest_bidder_id = ? WHERE id = ?";

        try (Connection conn = DbConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
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
    public void closeExpiredAuctions(java.sql.Timestamp currentTime) {
        // Thay vì dùng NOW(), ta dùng dấu ? để truyền giờ Java vào
        String sql = "UPDATE auctions SET status = 'FINISHED' WHERE status = 'RUNNING' AND end_time <= ?";

        try (Connection conn = DbConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, currentTime); // Đẩy giờ Java xuống Database

            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("[Hệ thống] Đã tự động ĐÓNG " + rowsUpdated + " phiên đấu giá hết hạn!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean createAuction(Auction auction) {
        String sql = "INSERT INTO auctions (item_id, start_time, end_time, bid_step, status) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DbConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

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
                return true;
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
        String sql = "SELECT a.*, i.id AS item_id, i.name AS item_name, i.item_type, "
                + "i.start_price, i.cur_highest "
                + "FROM auctions a "
                + "JOIN items i ON a.item_id = i.id "
                + "WHERE a.item_id = ? ORDER BY a.id DESC LIMIT 1";

        try (Connection conn = DbConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
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
                item.setCurHighest(rs.getDouble("cur_highest"));
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
        String sql = "SELECT a.*, i.name, i.item_type, i.start_price, i.cur_highest, i.image, u.full_name as seller_name " +
            "FROM auctions a " +
            "JOIN items i ON a.item_id = i.id " +
            "JOIN users u ON i.seller_id = u.id " +
            "WHERE a.end_time > ? AND a.status != 'CANCELLED' " +
            "ORDER BY a.end_time ASC";

        try (Connection conn = DbConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, java.sql.Timestamp.valueOf(LocalDateTime.now()));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    com.nhom3.shared.model.item.ItemType type =
                            com.nhom3.shared.model.item.ItemType.valueOf(rs.getString("item_type"));
                    Item item = type.createItem(
                            rs.getInt("item_id"),
                            rs.getString("name"),
                            rs.getDouble("start_price"));
                    item.setCurHighest(rs.getDouble("cur_highest"));
                    item.setSellerName(rs.getString("seller_name"));
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
        } catch (Exception e) {
            System.err.println("Lỗi khi tải danh sách Chợ Đấu Giá: ");
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public List<Auction> getAllAuctions() {
        List<Auction> list = new ArrayList<>();
        String sql = "SELECT a.*, i.name, i.item_type, i.start_price, i.cur_highest " +
                "FROM auctions a " +
                "JOIN items i ON a.item_id = i.id " +
                "ORDER BY a.id DESC";

        try (Connection conn = DbConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    com.nhom3.shared.model.item.ItemType type =
                            com.nhom3.shared.model.item.ItemType.valueOf(rs.getString("item_type"));
                    Item item = type.createItem(
                            rs.getInt("item_id"),
                            rs.getString("name"),
                            rs.getDouble("start_price"));
                    item.setCurHighest(rs.getDouble("cur_highest"));
                    int id = rs.getInt("id");
                    java.time.LocalDateTime start = rs.getTimestamp("start_time").toLocalDateTime();
                    java.time.LocalDateTime end = rs.getTimestamp("end_time").toLocalDateTime();
                    Auction auction = new Auction(id, item, start, end);
                    auction.setBidStep(readBidStep(rs));

                    java.time.LocalDateTime now = java.time.LocalDateTime.now();
                    String dbStatus = rs.getString("status");
                    if ("PAID".equals(dbStatus) || "CANCELLED".equals(dbStatus) || "FINISHED".equals(dbStatus)) {
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
                "a.bid_step, i.id AS item_id, i.name AS item_name, i.item_type, i.start_price, i.cur_highest, i.image, " +
                "my_bids.max_amount AS amount, my_bids.last_bid_time AS bid_time " +
                "FROM ( " +
                "    SELECT auction_id, MAX(amount) AS max_amount, MAX(bid_time) AS last_bid_time " +
                "    FROM bid_transactions " +
                "    WHERE bidder_id = ? " +
                "    GROUP BY auction_id " +
                ") my_bids " +
                "JOIN auctions a ON my_bids.auction_id = a.id " +
                "JOIN items i ON a.item_id = i.id " +
                "ORDER BY my_bids.last_bid_time DESC";

        try (Connection conn = DbConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, bidderId);
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
                item.setCurHighest(rs.getDouble("cur_highest"));
                item.setImageBase64(rs.getString("image"));

                LocalDateTime start = rs.getTimestamp("start_time").toLocalDateTime();
                LocalDateTime end = rs.getTimestamp("end_time").toLocalDateTime();
                Auction auction = new Auction(rs.getInt("auction_id"), item, start, end);
                auction.setBidStep(readBidStep(rs));

                // TÍNH TOÁN LẠI TRẠNG THÁI BẰNG JAVA
                String dbStatus = rs.getString("status");
                if ("PAID".equals(dbStatus) || "CANCELLED".equals(dbStatus)) {
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

    @Override
    public boolean cancelAuction(int auctionId) {
        String sql = "UPDATE auctions SET status = 'CANCELLED' WHERE id = ? AND status IN ('OPEN', 'RUNNING')";

        try (Connection conn = DbConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, auctionId);
            return stmt.executeUpdate() > 0;

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
    public void startScheduledAuctions(java.sql.Timestamp currentTime) {
        // Thay vì dùng NOW(), ta dùng dấu ? để truyền giờ Java vào
        String sql = "UPDATE auctions SET status = 'RUNNING' WHERE status = 'OPEN' AND start_time <= ?";

        try (Connection conn = DbConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, currentTime); // Đẩy giờ Java xuống Database

            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("[Hệ thống] Đã tự động MỞ " + rowsUpdated + " phiên đấu giá đến giờ lên sàn!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean saveAutoBidConfig(com.nhom3.shared.network.payload.AutoBidPayload payload) {
        // Cập nhật lại cấu hình nếu người dùng đã đặt Auto-Bid trước đó, nếu chưa thì
        // thêm mới
        String sqlCheck = "SELECT id FROM auto_bids WHERE bidder_id = ? AND auction_id = ?";
        String sqlUpdate = "UPDATE auto_bids SET max_amount = ?, increment_amount = ? WHERE bidder_id = ? AND auction_id = ?";
        String sqlInsert = "INSERT INTO auto_bids (bidder_id, auction_id, max_amount, increment_amount) VALUES (?, ?, ?, ?)";

        try (Connection conn = DbConnection.getConnection()) {
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
            String sqlTopBidder = "SELECT u.full_name, SUM(i.cur_highest) AS total_spent FROM auctions a JOIN items i ON a.item_id = i.id JOIN users u ON a.highest_bidder_id = u.id WHERE a.status = 'PAID' GROUP BY u.id, u.full_name ORDER BY total_spent DESC LIMIT 5";
            try (PreparedStatement stmt = conn.prepareStatement(sqlTopBidder); ResultSet rs = stmt.executeQuery()) {
                int rank = 1;
                while (rs.next())
                    topBidders.add(new com.nhom3.shared.network.payload.DashboardResponsePayload.TopBidderDTO(rank++,
                            rs.getString("full_name"), rs.getDouble("total_spent")));
            }
            // 5. Lấy Top 5 Sản phẩm đắt nhất (FINISHED hoặc PAID)
            String sqlTopItem = "SELECT i.name, i.cur_highest FROM auctions a JOIN items i ON a.item_id = i.id WHERE a.status IN ('PAID', 'FINISHED') ORDER BY i.cur_highest DESC LIMIT 5";
            try (PreparedStatement stmt = conn.prepareStatement(sqlTopItem); ResultSet rs = stmt.executeQuery()) {
                int rank = 1;
                while (rs.next())
                    topItems.add(new com.nhom3.shared.network.payload.DashboardResponsePayload.TopItemDTO(rank++,
                            rs.getString("name"), rs.getDouble("cur_highest")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new com.nhom3.shared.network.payload.DashboardResponsePayload(totalUsers, activeItems, totalRevenue,
                topBidders, topItems);
    }

    @Override
    public Auction getAuctionById(int auctionId) {
        String sql = "SELECT a.*, i.id AS item_id, i.name AS item_name, i.item_type, i.start_price, i.cur_highest, i.image " +
                "FROM auctions a JOIN items i ON a.item_id = i.id WHERE a.id = ?";
        try (Connection conn = DbConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, auctionId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                com.nhom3.shared.model.item.ItemType type = com.nhom3.shared.model.item.ItemType.valueOf(rs.getString("item_type"));
                Item item = type.createItem(rs.getInt("item_id"), rs.getString("item_name"), rs.getDouble("start_price"));
                item.setCurHighest(rs.getDouble("cur_highest"));
                item.setImageBase64(rs.getString("image"));

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
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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

    @Override
    public List<AutoBidPayload> getActiveAutoBids(int auctionId) {
        List<AutoBidPayload> list = new ArrayList<>();
        String sql = "SELECT bidder_id, max_amount, increment_amount FROM auto_bids WHERE auction_id = ? ORDER BY id ASC";
        try (Connection conn = DbConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, auctionId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(new AutoBidPayload(
                        rs.getInt("bidder_id"),
                        auctionId,
                        rs.getDouble("max_amount"),
                        rs.getDouble("increment_amount")));
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
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
