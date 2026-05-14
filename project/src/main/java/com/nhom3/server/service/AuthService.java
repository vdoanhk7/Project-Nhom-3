package com.nhom3.server.service;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.nhom3.server.dao.UserDAO;
import com.nhom3.server.dao.UserDAOImpl;
import com.nhom3.shared.model.user.User;

public class AuthService {
    private static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    private static final String PHONE_REGEX = "^0\\d{9}$";
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
        validateRegistrationData(user);
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

    private void validateRegistrationData(User user) {
        if (user == null || user.getUserInfo() == null || user.getUserContact() == null) {
            throw new IllegalArgumentException("Dữ liệu đăng ký không hợp lệ!");
        }

        String email = user.getUserContact().getEmail();
        String phone = user.getUserContact().getPhoneNumber();

        if (email == null || !email.matches(EMAIL_REGEX)) {
            throw new IllegalArgumentException("Email phải đúng định dạng hợp lệ.");
        }

        if (phone == null || !phone.matches(PHONE_REGEX)) {
            throw new IllegalArgumentException("Số điện thoại phải gồm đúng 10 số và bắt đầu bằng số 0");
        }
    }
}
