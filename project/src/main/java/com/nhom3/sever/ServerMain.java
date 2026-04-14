package com.nhom3.sever;

import com.nhom3.sever.dao.UserDAO;
import com.nhom3.sever.dao.UserDAOImpl;
import com.nhom3.sever.service.AuthService;
import com.nhom3.shared.model.user.Admin;
import com.nhom3.shared.model.user.User;
import com.nhom3.shared.model.user.UserContact;
import com.nhom3.shared.model.user.UserInfo;;

public class ServerMain {
    //test
    public static void main(String[] args) {
        UserDAO userDAO = new UserDAOImpl();
        AuthService authService = new AuthService(userDAO); // Truyền UserDAOimpl
        UserInfo userInfo = new UserInfo("testuser", "password123", "Test User");
        UserContact userContact = new UserContact("6ySdM@example.com", "1234567890");
        User user = new Admin(0, userInfo, userContact);
        authService.register(user);
        authService.login("testuser", "password123");
    }
}
