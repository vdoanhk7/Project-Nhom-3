package com.nhom3.server.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import java.sql.Connection;
import java.sql.SQLException;

public class DbConnection {
    // Use connection pool (HikariCP) instead of single connection for better
    // performance and scalability
    private static HikariDataSource dataSource;
    private static final Logger log = LoggerFactory.getLogger(DbConnection.class);

    static {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:mysql://auctiondb.c0vco82umoac.us-east-1.rds.amazonaws.com:3306/auction_db");
            config.setUsername("root");
            config.setPassword("hoathanhque");
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");

            config.setMaximumPoolSize(20);
            config.setMinimumIdle(5);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);

            dataSource = new HikariDataSource(config);
            log.info("[Server] Khởi tạo Connection Pool thành công.");

        } catch (Exception e) {
            log.error("[Server] Lỗi khởi tạo Connection Pool: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private DbConnection() {
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static void closePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            log.info("[Server] Đã đóng Connection Pool.");
        }
    }
}