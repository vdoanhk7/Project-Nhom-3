package com.nhom3.client.controller;
import com.nhom3.sever.service.AuthService;
import com.nhom3.shared.model.user.Bidder;
import com.nhom3.shared.model.user.Seller;
import com.nhom3.shared.model.user.User;
import com.nhom3.shared.model.user.UserContact;
import com.nhom3.shared.model.user.UserInfo;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class SignupController {

    @FXML private TextField txtFullName;
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private TextField txtEmail;
    @FXML private TextField txtPhone;
    @FXML private RadioButton radioBidder;
    @FXML private RadioButton radioSeller;

    // Sự kiện khi bấm nút "Đăng Ký"
    @FXML
    void handleSignup(ActionEvent event) {
        String fullName = txtFullName.getText();
        String username = txtUsername.getText();
        String password = txtPassword.getText();
        String confirmPass = txtConfirmPassword.getText();
        String email = txtEmail.getText().trim();
        String phone = txtPhone.getText().trim();


        // 1. Logic kiểm tra dữ liệu đầu vào
        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng nhập đủ tài khoản và mật khẩu!");
            return;
        }

        if (!password.equals(confirmPass)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi mật khẩu", "Mật khẩu xác nhận không khớp!");
            return;
        }

        //2. LOGIC DATABASE 
        UserInfo info = new UserInfo(username, password, fullName);
        UserContact contact = new UserContact(email, phone);
        User newUser;
        if (radioSeller != null && radioSeller.isSelected()) {
            newUser = new Seller(0, info, contact);
        } else {
            newUser = new Bidder(0, info, contact);
        }
        AuthService authService = new AuthService();
        // Gọi Server để lưu
        boolean isSuccess = authService.register(newUser);
        
        //3. Xử lý kết quả trả về từ Server
        if (isSuccess) {
            // Nếu qua được các bước kiểm tra trên -> Thành công
            System.out.println("Tạo tài khoản thành công cho: " + fullName);
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đăng ký tài khoản thành công! Vui lòng đăng nhập.");
            // Tự động quay lại trang đăng nhập 
            goToLogin(event);
        } else {
            // Nếu thất bại 
            showAlert(Alert.AlertType.ERROR, "Thất bại", "Tên đăng nhập đã tồn tại hoặc lỗi kết nối. Vui lòng thử lại!");
        }
    }

    // Sự kiện khi bấm nút "Đã có tài khoản? Đăng nhập"
    @FXML
    void goToLogin(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/nhom3/client/view/login.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Hàm hỗ trợ hiện thông báo cho ngắn gọn code
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}