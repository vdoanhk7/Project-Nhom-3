package com.nhom3.server.dao;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;


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
        try (Connection conn = DbConnection.getConnection();
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

    public List<Item> getItemsBySellerId(int sellerId) {
        List<Item> list = new ArrayList<>();
        String sql = "SELECT * FROM items WHERE seller_id = ?";
        
        try (Connection conn = DbConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {     
            stmt.setInt(1, sellerId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) { 
                int id = rs.getInt("id");
                String name = rs.getString("name");
                double startPrice = rs.getDouble("start_price");
                String type = rs.getString("item_type");
                double curHighest = rs.getDouble("cur_highest");
                Item item = null;
                switch (type) {
                    case "ART":
                        item = new com.nhom3.shared.factory.ArtCreator().createItem(id, name, startPrice);
                        break;
                    case "ELECTRONICS":
                        item = new com.nhom3.shared.factory.ElectronicsCreator().createItem(id, name, startPrice);
                        break;
                    case "VEHICLE":
                        item = new com.nhom3.shared.factory.VehicleCreator().createItem(id, name, startPrice);
                        break;
                }
                if (item != null) {
                    item.setCurHighest(curHighest);
                    list.add(item);
                }
            } 
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
        return list;
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
        String sql = "UPDATE items SET name = ?, start_price = ?, cur_highest = ?, item_type = ? WHERE id = ?"; 
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) { 
            stmt.setString(1, item.getName());
            stmt.setDouble(2, item.getStartPrice());
            stmt.setDouble(3, item.getCurHighest());  
            stmt.setString(4, item.getType());
            stmt.setInt(5, item.getId()); 
            return stmt.executeUpdate() > 0;     
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
