package com.nhom3;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.nhom3.server.dao.UserDAO;
import com.nhom3.server.dao.UserDAOImpl;
import com.nhom3.server.db.DbConnection;
import com.nhom3.shared.model.user.Bidder;
import com.nhom3.shared.model.user.Seller;
import com.nhom3.shared.model.user.User;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserDAOImplTest {

    @Test
    void testLogin_Success_AsSeller() throws Exception {
        // 1. Sử dụng mockStatic để chặn hàm getConnection() của DbConnection
        try (MockedStatic<DbConnection> mockedDb = mockStatic(DbConnection.class)) {
            // Giả lập các đối tượng JDBC
            Connection mockConn = mock(Connection.class);
            PreparedStatement mockStmt = mock(PreparedStatement.class);
            ResultSet mockRs = mock(ResultSet.class);

            // 2. Định nghĩa hành vi: Khi code gọi DbConnection.getConnection() -> trả về Connection giả
            mockedDb.when(DbConnection::getConnection).thenReturn(mockConn);
            when(mockConn.prepareStatement(anyString())).thenReturn(mockStmt);
            when(mockStmt.executeQuery()).thenReturn(mockRs);

            // Giả lập dữ liệu trả về từ Database cho vai trò SELLER
            when(mockRs.next()).thenReturn(true);
            when(mockRs.getInt("id")).thenReturn(1);
            when(mockRs.getString("role")).thenReturn("SELLER");
            when(mockRs.getString("username")).thenReturn("seller_test");
            when(mockRs.getString("password")).thenReturn("pass123");
            when(mockRs.getString("full_name")).thenReturn("Nguoi Ban Mau");

            // 3. Thực thi hàm cần kiểm thử
            UserDAO userDAO = new UserDAOImpl();
            User result = userDAO.login("seller_test", "pass123");

            // 4. Kiểm tra (Assertion)
            assertNotNull(result, "Đăng nhập đúng phải trả về đối tượng User");
            assertTrue(result instanceof Seller, "Phải thực hiện đúng Role Mapping sang Seller");
            assertEquals("Nguoi Ban Mau", result.getUserInfo().getName());
        }
    }

    @Test
    void testLogin_Failed_WrongPassword() throws Exception {
        try (MockedStatic<DbConnection> mockedDb = mockStatic(DbConnection.class)) {
            Connection mockConn = mock(Connection.class);
            PreparedStatement mockStmt = mock(PreparedStatement.class);
            ResultSet mockRs = mock(ResultSet.class);

            mockedDb.when(DbConnection::getConnection).thenReturn(mockConn);
            when(mockConn.prepareStatement(anyString())).thenReturn(mockStmt);
            when(mockStmt.executeQuery()).thenReturn(mockRs);

            // Giả lập database không tìm thấy bản ghi nào khớp (sai user/pass)
            when(mockRs.next()).thenReturn(false);

            UserDAO userDAO = new UserDAOImpl();
            User result = userDAO.login("user_sai", "pass_sai");

            assertNull(result, "Đăng nhập sai phải trả về null");
        }
    }

    @Test
    void testRegister_Success() throws Exception {
        try (MockedStatic<DbConnection> mockedDb = mockStatic(DbConnection.class)) {
            Connection mockConn = mock(Connection.class);
            PreparedStatement mockStmt = mock(PreparedStatement.class);

            mockedDb.when(DbConnection::getConnection).thenReturn(mockConn);
            when(mockConn.prepareStatement(anyString())).thenReturn(mockStmt);

            // Giả lập việc thực thi câu lệnh INSERT thành công
            when(mockStmt.executeUpdate()).thenReturn(1);

            // Tạo đối tượng User giả lập để đăng ký
            User bidder = new Bidder(0, null, null);

            UserDAO userDAO = new UserDAOImpl();
        }
    }
}