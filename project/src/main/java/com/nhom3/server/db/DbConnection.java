package com.nhom3.server.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbConnection {
    private static final Logger log = LoggerFactory.getLogger(DbConnection.class);
    private static final int DEFAULT_MAX_POOL_SIZE = 20;
    private static final int DEFAULT_MIN_IDLE_CONNECTIONS = 5;
    private static final long CONNECTION_TIMEOUT_MILLIS = 30_000L;
    private static final long IDLE_TIMEOUT_MILLIS = 600_000L;
    private static final long MAX_LIFETIME_MILLIS = 1_800_000L;
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
                "jdbc:mysql://mysql-1ee2c3cd-project3vn.k.aivencloud.com:19643/defaultdb"
                        + "?sslMode=REQUIRED"));
        config.setUsername(readConfig("AUCTION_DB_USER", "auction.db.user", "avnadmin"));
        config.setPassword(readConfig(
                "AUCTION_DB_PASSWORD",
                "auction.db.password",
                "AVNS_fgz_8u1cKk_gLZIoE_H"));
        config.setDriverClassName(readConfig(
                "AUCTION_DB_DRIVER", "auction.db.driver", "com.mysql.cj.jdbc.Driver"));

        config.setMaximumPoolSize(readIntConfig(
                "AUCTION_DB_POOL_MAX", "auction.db.pool.max", DEFAULT_MAX_POOL_SIZE));
        config.setMinimumIdle(readIntConfig(
                "AUCTION_DB_POOL_MIN", "auction.db.pool.min", DEFAULT_MIN_IDLE_CONNECTIONS));
        config.setConnectionTimeout(CONNECTION_TIMEOUT_MILLIS);
        config.setIdleTimeout(IDLE_TIMEOUT_MILLIS);
        config.setMaxLifetime(MAX_LIFETIME_MILLIS);

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
