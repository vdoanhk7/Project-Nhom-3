package com.nhom3.server;

import com.nhom3.server.service.AuctionMonitorService;
import com.nhom3.server.service.AuthService;
import com.nhom3.shared.model.user.Admin;
import com.nhom3.shared.model.user.User;
import com.nhom3.shared.model.user.UserContact;
import com.nhom3.shared.model.user.UserInfo;;

public class ServerMain {
    //test
        public static void main(String[] args) {
        AuctionMonitorService monitor = new AuctionMonitorService();
        monitor.startMonitoring();

        AuthService authService = new AuthService();
        UserInfo userInfo = new UserInfo("testuser", "password123", "Test User");
        UserContact userContact = new UserContact("6ySdM@example.com", "1234567890");
        User user = new Admin(0, userInfo, userContact);
        authService.register(user);
        authService.login("testuser", "password123");
    }
}
