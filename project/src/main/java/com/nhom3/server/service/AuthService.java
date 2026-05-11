package com.nhom3.server.service;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.nhom3.server.dao.UserDAO;
import com.nhom3.server.dao.UserDAOImpl;
import com.nhom3.shared.model.user.User;

public class AuthService {
    private final UserDAO userDAO;
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    public AuthService() {
        this.userDAO = new UserDAOImpl();
    }

    public AuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public User login(String username, String password) {
        User user = userDAO.login(username, password);
        if (user != null) {
            log.info("Server: " + user.getUserInfo().getName() + " đã đăng nhập thành công!");
        } else {
            log.info("Server: Đăng nhập thất bại. Sai tài khoản hoặc mật khẩu.");
        }
        return user;
    }
    public boolean register(User user) {
        boolean success = userDAO.register(user);
        if (success) {
            log.info("Server: " + user.getUserInfo().getName() + " đã đăng ký thành công với vai trò " + user.getRole() + "!");
        } else {
            log.info("Server: Đăng ký thất bại. Tài khoản có thể đã tồn tại.");
        }
        return success;
        
    }
    public boolean updateUser(User user) {
        return userDAO.updateUser(user);
    }
    public boolean changePassword(int userId, String newPassword) {
        return userDAO.updatePassword(userId, newPassword);
    }
}
