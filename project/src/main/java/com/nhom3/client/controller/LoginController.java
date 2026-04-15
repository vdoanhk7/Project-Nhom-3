package com.nhom3.client.controller;

import com.nhom3.client.utils.UserSession;
import com.nhom3.server.service.AuthService;
import com.nhom3.shared.model.user.User;
import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;

    // khi bấm nút "Đăng Nhập"
    @FXML
    void handleLogin(ActionEvent event) {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText();

        // 1. Kiểm tra trống
        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng nhập đầy đủ tài khoản và mật khẩu!");
            return;
        }
        // 2. Gọi Server để kiểm tra đăng nhập
        AuthService authService = new AuthService();
        // Trả về đối tượng User (Bidder/Seller/Admin) nếu đúng, trả về null nếu sai
        User loggedInUser = authService.login(username, password);
        // 3. Xử lý kết quả
        if (loggedInUser != null) {
            // Lưu thông tin người dùng vào Session để dùng cho toàn bộ app
            UserSession.getInstance().setLoggedInUser(loggedInUser);
            System.out.println(">> Đăng nhập thành công: " + loggedInUser.getUserInfo().getName());
            System.out.println(">> Vai trò: " + loggedInUser.getRole());
            // Chuyển sang màn hình chính
            goToMainLayout(event);
        } else {
            showAlert(Alert.AlertType.ERROR, "Thất bại", "Sai tên đăng nhập hoặc mật khẩu!");
        }
    }

    // Hàm chuyển trang sang Layout chính
    private void goToMainLayout(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/nhom3/client/view/layout_main.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Lỗi load file layout_main.fxml");
        }
    }

    // Hàm hỗ trợ thông báo
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // Sự kiện khi bấm nút "Đăng ký ngay"
    @FXML
    void goToSignup(ActionEvent event) {
        try {
            // 1. Nạp file signup.fxml
            Parent root = FXMLLoader.load(getClass().getResource("/com/nhom3/client/view/signup.fxml"));
        
            // 2. Lấy Stage hiện tại
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        
            // 3. Đặt Scene mới vào Stage
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}