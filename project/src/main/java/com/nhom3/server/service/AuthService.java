package com.nhom3.server.service;

import com.nhom3.server.dao.UserDAO;
import com.nhom3.server.dao.UserDAOImpl;
import com.nhom3.shared.model.user.User;

public class AuthService {
    private final UserDAO userDAO;

    public AuthService() {
        this.userDAO = new UserDAOImpl();
    }

    public User login(String username, String password) {
        User user = userDAO.login(username, password);
        if (user != null) {
            System.out.println("Server: " + user.getUserInfo().getName() + " đã đăng nhập thành công!");
        } else {
            System.out.println("Server: Đăng nhập thất bại. Sai tài khoản hoặc mật khẩu.");
        }
        return user;
    }
    public boolean register(User user) {
        boolean success = userDAO.register(user);
        if (success) {
            System.out.println("Server: " + user.getUserInfo().getName() + " đã đăng ký thành công với vai trò " + user.getRole() + "!");
        } else {
            System.out.println("Server: Đăng ký thất bại. Tài khoản có thể đã tồn tại.");
        }
        return success;
        
    }
    public boolean updateUser(User user) {
        return userDAO.updateUser(user);
    }
}
