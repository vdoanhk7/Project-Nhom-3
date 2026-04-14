package com.nhom3.sever.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DbConnection {
    private static final String URL = "jdbc:mysql://auctiondb.c0vco82umoac.us-east-1.rds.amazonaws.com:3306/auction_db";
    private static final String USER = "admin";
    private static final String PASS = "hoathanhque";

    private static Connection instance;

    private DbConnection() {}

    public static Connection getInstance() {
        try {
            if (instance == null || instance.isClosed()) {
                Class.forName("com.mysql.cj.jdbc.Driver");
                instance = DriverManager.getConnection(URL, USER, PASS);
                System.out.println("[Server] Đã kết nối thành công tới MySQL Cloud!");
            }
        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("[Server] Lỗi kết nối Database: " + e.getMessage());
        }
        return instance;
    }
}