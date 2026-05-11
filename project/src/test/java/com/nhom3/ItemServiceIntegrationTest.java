package com.nhom3;

import com.nhom3.server.dao.AuctionDAOImpl;
import com.nhom3.server.dao.ItemDAOImpl;
import com.nhom3.server.service.ItemService;
import com.nhom3.shared.model.item.Art;
import com.nhom3.shared.model.item.Item;
import com.nhom3.shared.model.user.Seller;
import com.nhom3.shared.model.user.UserContact;
import com.nhom3.shared.model.user.UserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

public class ItemServiceIntegrationTest extends IntegrationTestBase {

    private ItemDAOImpl itemDAO;
    private AuctionDAOImpl auctionDAO;
    private ItemService itemService;

    @BeforeEach
    public void setUp() {
        // Initialize real DAOs and Service
        itemDAO = new ItemDAOImpl();
        auctionDAO = new AuctionDAOImpl();
        itemService = new ItemService(itemDAO, auctionDAO);
    }

    @Test
    public void testCreateAndRemoveItemIntegration() throws Exception {
        // 1. Setup a Seller in the DB manually using connection
        int sellerId;
        try (PreparedStatement stmt = h2Connection.prepareStatement(
                "INSERT INTO users (username, password, full_name, email, phone, role) VALUES (?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, "seller1");
            stmt.setString(2, "hashedpwd");
            stmt.setString(3, "Seller Name");
            stmt.setString(4, "seller@example.com");
            stmt.setString(5, "0987654321");
            stmt.setString(6, "SELLER");
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                assertTrue(rs.next());
                sellerId = rs.getInt(1);
            }
        }

        // Create a Seller model object to pass to service
        UserInfo info = new UserInfo("seller1", "pwd", "Seller Name");
        UserContact contact = new UserContact("seller@example.com", "0987654321");
        Seller seller = new Seller(sellerId, info, contact);

        // 2. Perform createItem via ItemService
        Item artItem = new Art(0, "Mona Lisa Replica", 1000.0);
        boolean isCreated = itemService.createItem(seller, artItem);
        assertTrue(isCreated, "Item should be created successfully");

        // 3. Verify Item exists in DB
        int itemId = -1;
        try (Statement stmt = h2Connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM items WHERE name = 'Mona Lisa Replica'")) {
            assertTrue(rs.next(), "Item should be present in the database");
            assertEquals(1000.0, rs.getDouble("start_price"));
            assertEquals("ART", rs.getString("item_type"));
            itemId = rs.getInt("id");
        }
        
        assertTrue(itemId > 0, "Item ID should have been generated");

        // 4. Test remove item via ItemService
        boolean isRemoved = itemService.removeItem(sellerId, itemId);
        assertTrue(isRemoved, "Item should be removed successfully");

        // 5. Verify Item no longer exists in DB
        try (Statement stmt = h2Connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM items WHERE id = " + itemId)) {
            assertFalse(rs.next(), "Item should be deleted from the database");
        }
    }
}
