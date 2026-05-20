package com.nhom3.server.dao;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


import com.nhom3.server.db.DbConnection;
import com.nhom3.shared.model.item.Item;
import com.nhom3.shared.network.payload.SellerItemsResponsePayload;

public class ItemDAOImpl implements ItemDAO {
    @Override
    public boolean saveItem(Item item, int sellerId) {
        String sql = "INSERT INTO items (seller_id, name, start_price, cur_highest, item_type, image, description) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        if (item == null || sellerId <= 0) {
            return false;
        }
        try (Connection conn = DbConnection.getConnection()) {
            ensureDescriptionColumn(conn);
            try (PreparedStatement stmt = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, sellerId);
                stmt.setString(2, item.getName());
                stmt.setDouble(3, item.getStartPrice());
                stmt.setDouble(4, item.getCurHighest());
                stmt.setString(5, item.getType().name());
                stmt.setString(6, item.getImageBase64());
                stmt.setString(7, item.getDescription());

                int affectedRows = stmt.executeUpdate();
                if (affectedRows > 0) {
                    try (ResultSet rs = stmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            item.setId(rs.getInt(1));
                        }
                    }
                } else {
                    return false;
                }
            }
        }
            catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        
        return true;
    }

    public List<Item> getItemsBySellerId(int sellerId) {
        List<Item> list = new ArrayList<>();
        String sql = "SELECT id, name, description, start_price, cur_highest, item_type FROM items WHERE seller_id = ?";
        
        try (Connection conn = DbConnection.getConnection()) {
            ensureDescriptionColumn(conn);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, sellerId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) { 
                int id = rs.getInt("id");
                String name = rs.getString("name");
                double startPrice = rs.getDouble("start_price");
                String type = rs.getString("item_type");
                double curHighest = rs.getDouble("cur_highest");
                Item item = null;
                com.nhom3.shared.model.item.ItemType itemType = com.nhom3.shared.model.item.ItemType.valueOf(type);
                item = itemType.createItem(id, name, startPrice);
                if (item != null) {
                    item.setDescription(rs.getString("description"));
                    item.setCurHighest(curHighest);
                    list.add(item);
                }
            }
            }
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
        return list;
    }

    @Override
    public List<SellerItemsResponsePayload.SellerItemDTO> getSellerItemsForManagement(int sellerId) {
        List<SellerItemsResponsePayload.SellerItemDTO> list = new ArrayList<>();
        String sql = "SELECT i.id, i.name, i.description, i.start_price, i.cur_highest, i.item_type, "
                + "a.highest_bidder_id, "
                + "CASE "
                + "  WHEN a.id IS NULL THEN '' "
                + "  WHEN a.status IN ('PAID', 'CANCELLED', 'FINISHED') THEN a.status "
                + "  WHEN ? < a.start_time THEN 'OPEN' "
                + "  WHEN ? >= a.end_time THEN 'FINISHED' "
                + "  ELSE 'RUNNING' "
                + "END AS real_status "
                + "FROM items i "
                + "LEFT JOIN auctions a ON a.id = ( "
                + "  SELECT MAX(a2.id) "
                + "  FROM auctions a2 "
                + "  WHERE a2.item_id = i.id "
                + ") "
                + "WHERE i.seller_id = ? "
                + "ORDER BY i.id DESC";

        try (Connection conn = DbConnection.getConnection()) {
            ensureDescriptionColumn(conn);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                Timestamp currentTime = Timestamp.valueOf(LocalDateTime.now());
                stmt.setTimestamp(1, currentTime);
                stmt.setTimestamp(2, currentTime);
                stmt.setInt(3, sellerId);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        int highestBidderId = rs.getInt("highest_bidder_id");
                        if (rs.wasNull()) {
                            highestBidderId = -1;
                        }
                        list.add(new SellerItemsResponsePayload.SellerItemDTO(
                                rs.getInt("id"),
                                rs.getString("name"),
                                rs.getString("description"),
                                rs.getString("item_type"),
                                rs.getDouble("start_price"),
                                rs.getDouble("cur_highest"),
                                rs.getString("real_status"),
                                null,
                                highestBidderId));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public String getItemImageBase64(int itemId) {
        String sql = "SELECT image FROM items WHERE id = ?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, itemId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("image");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean deleteItem(int itemId, int sellerId) {
        String sql = "DELETE FROM items WHERE id = ? AND seller_id = ?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {  
            stmt.setInt(1, itemId);
            stmt.setInt(2, sellerId);
            int rowsAffected = stmt.executeUpdate();   
            return rowsAffected > 0;          
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean updateItem(Item item) {
        String sql;
        boolean shouldUpdateImage = item.getImageBase64() != null;
        if (shouldUpdateImage) {
            sql = "UPDATE items SET name = ?, start_price = ?, cur_highest = ?, item_type = ?, image = ?, description = ? WHERE id = ?";
        } else {
            sql = "UPDATE items SET name = ?, start_price = ?, cur_highest = ?, item_type = ?, description = ? WHERE id = ?";
        }
        try (Connection conn = DbConnection.getConnection()) {
            ensureDescriptionColumn(conn);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, item.getName());
            stmt.setDouble(2, item.getStartPrice());
            stmt.setDouble(3, item.getCurHighest());  
            stmt.setString(4, item.getType().name());
            if (shouldUpdateImage) {
                stmt.setString(5, item.getImageBase64());
                stmt.setString(6, item.getDescription());
                stmt.setInt(7, item.getId());
            } else {
                stmt.setString(5, item.getDescription());
                stmt.setInt(6, item.getId());
            }
            return stmt.executeUpdate() > 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
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
}
