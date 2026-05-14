package com.nhom3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.nhom3.server.dao.UserDAO;
import com.nhom3.server.service.AuthService;
import com.nhom3.shared.model.user.Role;
import com.nhom3.shared.model.user.User;
import com.nhom3.shared.model.user.UserContact;
import com.nhom3.shared.model.user.UserInfo;

public class AuthServiceTest {

    @Mock
    private UserDAO userDAO;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authService = new AuthService(userDAO);
    }

    @Test
    void login_Success_ShouldReturnUser() {
        User mockUser = mock(User.class);
        UserInfo mockUserInfo = mock(UserInfo.class);
        when(mockUserInfo.getName()).thenReturn("Test User");
        when(mockUser.getUserInfo()).thenReturn(mockUserInfo);

        when(userDAO.login("testuser", "password")).thenReturn(mockUser);

        User result = authService.login("testuser", "password");

        assertNotNull(result);
        assertEquals(mockUser, result);
        verify(userDAO).login("testuser", "password");
    }

    @Test
    void login_Failure_ShouldReturnNull() {
        when(userDAO.login(anyString(), anyString())).thenReturn(null);

        User result = authService.login("wronguser", "wrongpass");

        assertNull(result);
        verify(userDAO).login("wronguser", "wrongpass");
    }

    @Test
    void register_Success_ShouldReturnTrue() {
        User mockUser = createValidUser();

        when(userDAO.register(mockUser)).thenReturn(true);

        boolean result = authService.register(mockUser);

        assertTrue(result);
        verify(userDAO).register(mockUser);
    }

    @Test
    void register_Failure_ShouldReturnFalse() {
        User mockUser = createValidUser();
        when(userDAO.register(mockUser)).thenReturn(false);

        boolean result = authService.register(mockUser);

        assertFalse(result);
        verify(userDAO).register(mockUser);
    }

    @Test
    void updateUser_Success_ShouldReturnTrue() {
        User mockUser = mock(User.class);
        when(userDAO.updateUser(mockUser)).thenReturn(true);

        boolean result = authService.updateUser(mockUser);

        assertTrue(result);
        verify(userDAO).updateUser(mockUser);
    }

    @Test
    void updateUser_Failure_ShouldReturnFalse() {
        User mockUser = mock(User.class);
        when(userDAO.updateUser(mockUser)).thenReturn(false);

        boolean result = authService.updateUser(mockUser);

        assertFalse(result);
        verify(userDAO).updateUser(mockUser);
    }

    @Test
    void changePassword_Success_ShouldReturnTrue() {
        when(userDAO.updatePassword(1, "newPass")).thenReturn(true);

        boolean result = authService.changePassword(1, "newPass");

        assertTrue(result);
        verify(userDAO).updatePassword(1, "newPass");
    }

    @Test
    void changePassword_Failure_ShouldReturnFalse() {
        when(userDAO.updatePassword(1, "newPass")).thenReturn(false);

        boolean result = authService.changePassword(1, "newPass");

        assertFalse(result);
        verify(userDAO).updatePassword(1, "newPass");
    }

    private User createValidUser() {
        User mockUser = mock(User.class);
        UserInfo mockUserInfo = mock(UserInfo.class);
        UserContact mockUserContact = mock(UserContact.class);

        when(mockUserInfo.getName()).thenReturn("Test User");
        when(mockUserContact.getEmail()).thenReturn("test@example.com");
        when(mockUserContact.getPhoneNumber()).thenReturn("0123456789");
        when(mockUser.getUserInfo()).thenReturn(mockUserInfo);
        when(mockUser.getUserContact()).thenReturn(mockUserContact);
        when(mockUser.getRole()).thenReturn(Role.BIDDER);

        return mockUser;
    }
}
