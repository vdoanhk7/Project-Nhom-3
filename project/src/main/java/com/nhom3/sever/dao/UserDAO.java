package com.nhom3.sever.dao;

import com.nhom3.shared.model.user.User;

public interface UserDAO {
    User login(String username, String password);
    
    boolean register(User user);
}
