package com.nhom3;

import com.nhom3.server.dao.UserDAOImpl;
import com.nhom3.server.db.DbConnection;
import com.nhom3.shared.model.user.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class UserDAOImplTest {

    private UserDAOImpl userDAO;

    private Connection conn;
    private PreparedStatement stmt;
    private ResultSet rs;

    @BeforeEach
    void setUp() throws Exception {
        userDAO = new UserDAOImpl();

        conn = mock(Connection.class);
        stmt = mock(PreparedStatement.class);
        rs = mock(ResultSet.class);
    }

    @Test
    void testLoginSuccessBidder() throws Exception {

        String password = "123456";
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        try (MockedStatic<DbConnection> mockedDb =
                     Mockito.mockStatic(DbConnection.class)) {

            mockedDb.when(DbConnection::getConnection).thenReturn(conn);

            when(conn.prepareStatement(anyString())).thenReturn(stmt);

            when(stmt.executeQuery()).thenReturn(rs);

            when(rs.next()).thenReturn(true);

            when(rs.getString("password")).thenReturn(hashedPassword);
            when(rs.getInt("id")).thenReturn(1);
            when(rs.getString("role")).thenReturn("BIDDER");

            when(rs.getString("username")).thenReturn("dung");
            when(rs.getString("full_name")).thenReturn("Pham Dung");
            when(rs.getString("email")).thenReturn("dung@gmail.com");
            when(rs.getString("phone")).thenReturn("0123456789");

            User result = userDAO.login("dung", password);

            assertNotNull(result);
            assertTrue(result instanceof Bidder);
            assertEquals(1, result.getId());
        }
    }

    @Test
    void testLoginWrongPassword() throws Exception {

        String hashedPassword =
                BCrypt.hashpw("correctPassword", BCrypt.gensalt());

        try (MockedStatic<DbConnection> mockedDb =
                     Mockito.mockStatic(DbConnection.class)) {

            mockedDb.when(DbConnection::getConnection).thenReturn(conn);

            when(conn.prepareStatement(anyString())).thenReturn(stmt);

            when(stmt.executeQuery()).thenReturn(rs);

            when(rs.next()).thenReturn(true);

            when(rs.getString("password")).thenReturn(hashedPassword);

            User result = userDAO.login("dung", "wrongPassword");

            assertNull(result);
        }
    }

    @Test
    void testRegisterSuccess() throws Exception {

        UserInfo info = new UserInfo(
                "dung",
                "123456",
                "Pham Dung"
        );

        UserContact contact = new UserContact(
                "dung@gmail.com",
                "0123456789"
        );

        User user = new Bidder(1, info, contact);

        try (MockedStatic<DbConnection> mockedDb =
                     Mockito.mockStatic(DbConnection.class)) {

            mockedDb.when(DbConnection::getConnection).thenReturn(conn);

            when(conn.prepareStatement(anyString())).thenReturn(stmt);

            when(stmt.executeUpdate()).thenReturn(1);

            boolean result = userDAO.register(user);

            assertTrue(result);

            verify(stmt).setString(eq(1), eq("dung"));
            verify(stmt).setString(eq(3), eq("Pham Dung"));
        }
    }

    @Test
    void testUpdateUserSuccess() throws Exception {

        UserInfo info = new UserInfo(
                "dung",
                "123456",
                "New Name"
        );

        UserContact contact = new UserContact(
                "new@gmail.com",
                "0999999999"
        );

        User user = new Bidder(1, info, contact);

        try (MockedStatic<DbConnection> mockedDb =
                     Mockito.mockStatic(DbConnection.class)) {

            mockedDb.when(DbConnection::getConnection).thenReturn(conn);

            when(conn.prepareStatement(anyString())).thenReturn(stmt);

            when(stmt.executeUpdate()).thenReturn(1);

            boolean result = userDAO.updateUser(user);

            assertTrue(result);

            verify(stmt).setString(1, "New Name");
            verify(stmt).setString(2, "new@gmail.com");
            verify(stmt).setString(3, "0999999999");
            verify(stmt).setInt(4, 1);
        }
    }

    @Test
    void testUpdatePasswordSuccess() throws Exception {

        try (MockedStatic<DbConnection> mockedDb =
                     Mockito.mockStatic(DbConnection.class)) {

            mockedDb.when(DbConnection::getConnection).thenReturn(conn);

            when(conn.prepareStatement(anyString())).thenReturn(stmt);

            when(stmt.executeUpdate()).thenReturn(1);

            boolean result =
                    userDAO.updatePassword(1, "newPassword");

            assertTrue(result);

            verify(stmt).setInt(2, 1);
        }
    }
}