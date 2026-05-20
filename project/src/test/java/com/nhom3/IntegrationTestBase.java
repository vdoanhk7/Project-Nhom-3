package com.nhom3;

import com.nhom3.server.db.DbConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public abstract class IntegrationTestBase {

    protected Connection h2Connection;
    protected MockedStatic<DbConnection> mockedDbConnection;

    @BeforeEach
    public void setUpDb() throws Exception {
        // Create initial connection to initialize schema
        h2Connection = DriverManager.getConnection("jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");

        // Initialize schema
        try (Statement stmt = h2Connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "username VARCHAR(50) NOT NULL UNIQUE," +
                    "password VARCHAR(255) NOT NULL," +
                    "full_name VARCHAR(100) NOT NULL," +
                    "email VARCHAR(100)," +
                    "phone VARCHAR(20)," +
                    "reputation_score INT NOT NULL DEFAULT 100," +
                    "role VARCHAR(20) NOT NULL," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ");");

            stmt.execute("CREATE TABLE IF NOT EXISTS items (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "seller_id INT NOT NULL," +
                    "name VARCHAR(255) NOT NULL," +
                    "description TEXT," +
                    "start_price DOUBLE NOT NULL," +
                    "cur_highest DOUBLE DEFAULT 0," +
                    "item_type VARCHAR(50) NOT NULL," +
                    "image MEDIUMTEXT," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE" +
                    ");");

            stmt.execute("CREATE TABLE IF NOT EXISTS auctions (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "item_id INT NOT NULL," +
                    "start_time DATETIME NOT NULL," +
                    "end_time DATETIME NOT NULL," +
                    "bid_step DOUBLE NOT NULL DEFAULT 50000," +
                    "status VARCHAR(20) NOT NULL DEFAULT 'OPEN'," +
                    "highest_bidder_id INT," +
                    "reputation_penalty INT NOT NULL DEFAULT 0," +
                    "FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE," +
                    "FOREIGN KEY (highest_bidder_id) REFERENCES users(id) ON DELETE SET NULL" +
                    ");");

            stmt.execute("CREATE TABLE IF NOT EXISTS bid_transactions (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "auction_id INT NOT NULL," +
                    "bidder_id INT NOT NULL," +
                    "amount DOUBLE NOT NULL," +
                    "bid_time DATETIME NOT NULL," +
                    "note VARCHAR(255)," +
                    "FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE," +
                    "FOREIGN KEY (bidder_id) REFERENCES users(id) ON DELETE CASCADE" +
                    ");");

            stmt.execute("CREATE TABLE IF NOT EXISTS seller_ratings (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "auction_id INT NOT NULL," +
                    "buyer_id INT NOT NULL," +
                    "seller_id INT NOT NULL," +
                    "stars INT NOT NULL," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "UNIQUE (auction_id, buyer_id)," +
                    "FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE," +
                    "FOREIGN KEY (buyer_id) REFERENCES users(id) ON DELETE CASCADE," +
                    "FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE" +
                    ");");
        }

        // Mock DbConnection to return new H2 connection each time
        mockedDbConnection = Mockito.mockStatic(DbConnection.class);
        mockedDbConnection.when(DbConnection::getConnection).thenAnswer(
                invocation -> DriverManager.getConnection("jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
    }

    @AfterEach
    public void tearDownDb() throws Exception {
        if (mockedDbConnection != null) {
            mockedDbConnection.close();
        }

        // Clean up database tables for the next test
        if (h2Connection != null && !h2Connection.isClosed()) {
            try (Statement stmt = h2Connection.createStatement()) {
                stmt.execute("DROP ALL OBJECTS");
            }
            h2Connection.close();
        }
    }
}
