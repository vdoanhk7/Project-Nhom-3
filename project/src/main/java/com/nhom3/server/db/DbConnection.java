package com.nhom3.server.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbConnection {
    private static final Logger log = LoggerFactory.getLogger(DbConnection.class);
    private static volatile HikariDataSource dataSource;

    private DbConnection() {
    }

    public static Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    public static synchronized void closePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            dataSource = null;
            log.info("[Server] Da dong connection pool.");
        }
    }

    private static HikariDataSource getDataSource() {
        HikariDataSource current = dataSource;
        if (current == null || current.isClosed()) {
            synchronized (DbConnection.class) {
                current = dataSource;
                if (current == null || current.isClosed()) {
                    dataSource = createDataSource();
                    current = dataSource;
                }
            }
        }
        return current;
    }

    private static HikariDataSource createDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(readConfig(
                "AUCTION_DB_URL",
                "auction.db.url",
                "jdbc:mysql://localhost:3306/auction_db?useSSL=false"
                        + "&allowPublicKeyRetrieval=true&serverTimezone=Asia/Bangkok"));
        config.setUsername(readConfig("AUCTION_DB_USER", "auction.db.user", "root"));
        config.setPassword(readConfig("AUCTION_DB_PASSWORD", "auction.db.password", ""));
        config.setDriverClassName(readConfig(
                "AUCTION_DB_DRIVER", "auction.db.driver", "com.mysql.cj.jdbc.Driver"));

        config.setMaximumPoolSize(readIntConfig("AUCTION_DB_POOL_MAX", "auction.db.pool.max", 20));
        config.setMinimumIdle(readIntConfig("AUCTION_DB_POOL_MIN", "auction.db.pool.min", 5));
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        HikariDataSource hikariDataSource = new HikariDataSource(config);
        log.info("[Server] Khoi tao connection pool thanh cong.");
        return hikariDataSource;
    }

    private static String readConfig(String envName, String propertyName, String defaultValue) {
        String propertyValue = System.getProperty(propertyName);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue;
        }
        String envValue = System.getenv(envName);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        return defaultValue;
    }

    private static int readIntConfig(String envName, String propertyName, int defaultValue) {
        String value = readConfig(envName, propertyName, Integer.toString(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
