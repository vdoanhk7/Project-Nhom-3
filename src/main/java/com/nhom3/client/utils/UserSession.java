package com.nhom3.client.utils;

import com.nhom3.shared.model.user.User;

// Singleton để quản lý phiên đăng nhập của người dùng trong ứng dụng JavaFX 
public class UserSession {
    private static UserSession instance;
    private User loggedInUser;
  
    private UserSession() {}

    public static UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    public User getLoggedInUser() {
        return loggedInUser;
    }

    public void setLoggedInUser(User user) {
        this.loggedInUser = user;
    }

    public void logout() {
        this.loggedInUser = null;
    }
}