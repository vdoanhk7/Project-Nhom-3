package com.nhom3.client.controller;

import com.nhom3.client.utils.UserSession;
import com.nhom3.server.service.AuthService;
import com.nhom3.shared.model.user.User;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;

public class ChangePasswordController {

    @FXML private PasswordField txtOldPass;
    @FXML private PasswordField txtNewPass;
    @FXML private PasswordField txtConfirmPass;
    @FXML private Button btnCancel;

    // Khi bấm nút "Cập nhật"
    @FXML
    void handleSavePassword(ActionEvent event) {
        String oldPass = txtOldPass.getText();
        String newPass = txtNewPass.getText();
        String confirmPass = txtConfirmPass.getText();

        if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng nhập đầy đủ thông tin vào các ô!");
            return;
        }

        User currentUser = UserSession.getInstance().getLoggedInUser();
        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống", "Không tìm thấy phiên đăng nhập. Vui lòng đăng nhập lại!");
            return;
        }

        String currentSavedPass = currentUser.getUserInfo().getPassword();
        if (!currentSavedPass.equals(oldPass)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi xác thực", "Mật khẩu hiện tại không chính xác!");
            return;
        }

        if (!newPass.equals(confirmPass)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi nhập liệu", "Mật khẩu xác nhận không khớp!");
            return;
        }

        if (newPass.equals(oldPass)) {
            showAlert(Alert.AlertType.WARNING, "Thông báo", "Mật khẩu mới phải khác với mật khẩu cũ!");
            return;
        }

        AuthService authService = new AuthService(); 
        
        boolean isSuccess = authService.changePassword(currentUser.getId(), newPass);

        if (isSuccess) {
            currentUser.getUserInfo().setPassword(newPass);
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Mật khẩu đã được thay đổi thành công!");
            closeWindow();
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi Database", "Không thể cập nhật mật khẩu vào hệ thống. Vui lòng thử lại!");
        }
    }

    // Hàm hỗ trợ: 
    @FXML
    void handleCancel(ActionEvent event) {
        closeWindow();
    }
    private void closeWindow() {
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}