package com.nhom3.server.dao;

import com.nhom3.shared.model.user.User;

public interface UserDAO {
    User login(String username, String password);
    
    boolean register(User user);
    boolean updateUser(User user);
    boolean updatePassword(int userId, String newPassword);
}
