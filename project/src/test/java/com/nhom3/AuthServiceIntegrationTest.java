package com.nhom3;

import com.nhom3.server.dao.UserDAOImpl;
import com.nhom3.server.service.AuthService;
import com.nhom3.shared.model.user.Bidder;
import com.nhom3.shared.model.user.User;
import com.nhom3.shared.model.user.UserContact;
import com.nhom3.shared.model.user.UserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AuthServiceIntegrationTest extends IntegrationTestBase {

    private UserDAOImpl userDAO;
    private AuthService authService;

    @BeforeEach
    public void setUp() {
        // Initialize real DAO and Service
        userDAO = new UserDAOImpl();
        authService = new AuthService(userDAO);
    }

    @Test
    public void testRegisterAndLoginIntegration() {
        // 1. Prepare user data
        UserInfo userInfo = new UserInfo("testuser", "securePassword123", "Test User Name");
        UserContact userContact = new UserContact("test@example.com", "0123456789");
        Bidder newBidder = new Bidder(0, userInfo, userContact); // 0 because ID is auto-incremented
        
        // 2. Perform Register via AuthService
        boolean isRegistered = authService.register(newBidder);
        assertTrue(isRegistered, "User should be registered successfully");
        
        // 3. Attempt to login with correct credentials
        User loggedInUser = authService.login("testuser", "securePassword123");
        assertNotNull(loggedInUser, "Login should be successful and return a valid User object");
        
        // 4. Verify the retrieved user data
        assertEquals("testuser", loggedInUser.getUserInfo().getUserName());
        assertEquals("Test User Name", loggedInUser.getUserInfo().getName());
        assertEquals(com.nhom3.shared.model.user.Role.BIDDER, loggedInUser.getRole());
        assertTrue(loggedInUser.getId() > 0, "Generated ID should be greater than 0");
        
        // 5. Attempt to login with wrong password
        User wrongPasswordUser = authService.login("testuser", "wrongpassword");
        assertNull(wrongPasswordUser, "Login should fail with wrong password");
        
        // 6. Attempt to login with non-existent user
        User notFoundUser = authService.login("nonexistent", "securePassword123");
        assertNull(notFoundUser, "Login should fail for non-existent user");
    }
}
