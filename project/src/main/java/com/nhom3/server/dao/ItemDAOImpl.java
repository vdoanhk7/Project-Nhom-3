package com.nhom3.server.dao;
import java.sql.Connection;
import java.sql.PreparedStatement;

import com.nhom3.server.db.DbConnection;
import com.nhom3.shared.model.item.Item;

public class ItemDAOImpl implements ItemDAO {
    @Override
    public boolean saveItem(Item item, int sellerId) {
        String sql = "INSERT INTO items (seller_id, name, start_price, cur_highest, item_type) " +
                     "VALUES (?, ?, ?, ?, ?)";
        if (item == null || sellerId <= 0) {
            return false;
        }
        try (Connection conn = DbConnection.getInstance();
            PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, sellerId);
            stmt.setString(2, item.getName());
            stmt.setDouble(3, item.getStartPrice());
            stmt.setDouble(4, item.getCurHighest());
            stmt.setString(5, item.getType());
            stmt.executeUpdate();
            }
            catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        
        return true;
    }
}
