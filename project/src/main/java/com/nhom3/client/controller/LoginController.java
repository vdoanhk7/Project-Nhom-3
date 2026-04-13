package com.nhom3.client.controller;

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

    @FXML
    private TextField txtUsername;

    @FXML
    private PasswordField txtPassword;

    // khi bấm nút "Đăng Nhập"
    @FXML
    void handleLogin(ActionEvent event) {
        String username = txtUsername.getText();
        String password = txtPassword.getText();

        // Kiểm tra tài khoản (Sau này sẽ thay bằng code kết nối Database)
        if ("admin".equals(username) && "12345".equals(password)) {
            System.out.println("Đăng nhập thành công!");
            
            try {
                // Tải màn hình chính
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/nhom3/client/view/layout_main.fxml"));
                Parent root = loader.load();
                
                // Chuyển cảnh
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root, 1000, 700)); // Kích thước màn hình chính
                stage.centerOnScreen();
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // Hiện thông báo lỗi thay vì chỉ in ra console
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi đăng nhập");
            alert.setHeaderText(null);
            alert.setContentText("Sai tên đăng nhập hoặc mật khẩu. Vui lòng thử lại!");
            alert.showAndWait();
        }
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