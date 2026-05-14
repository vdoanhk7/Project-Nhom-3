package com.nhom3.client.controller;

import com.nhom3.client.network.ServerConnection;
import com.nhom3.shared.network.packet.Packet;
import com.nhom3.shared.network.packet.PacketType;
import com.nhom3.shared.network.payload.RegisterPayload;

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

@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
        value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD",
        justification = "JavaFX controllers are reached from the socket dispatcher through the active screen instance.")
public class SignupController {

    @FXML private TextField txtFullName;
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private TextField txtEmail;
    @FXML private TextField txtPhone;
    @FXML private RadioButton radioBidder;
    @FXML private RadioButton radioSeller;

    // 1. TẠO SINGLETON ĐỂ LUỒNG MẠNG CÓ THỂ GỌI ĐẾN
    private static SignupController instance;
    private ActionEvent currentEvent; // Lưu lại sự kiện click để lát chuyển trang

    @FXML
    public void initialize() {
        instance = this;
    }

    public static SignupController getInstance() {
        return instance;
    }

    @FXML
    void handleSignup(ActionEvent event) {
        String fullName = txtFullName.getText();
        String username = txtUsername.getText();
        String password = txtPassword.getText();
        String confirmPass = txtConfirmPassword.getText();
        String email = txtEmail.getText().trim();
        String phone = txtPhone.getText().trim();

        if (username.isEmpty() || password.isEmpty() || fullName.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        if (!password.equals(confirmPass)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi mật khẩu", "Mật khẩu xác nhận không khớp!");
            return;
        }

        this.currentEvent = event; // Lưu lại event để chuyển trang sau khi đăng ký thành công

        // 2. GÓI DỮ LIỆU VÀO REGISTER PAYLOAD (Thay vì gọi thẳng DB)
        String roleStr = (radioSeller != null && radioSeller.isSelected()) ? "SELLER" : "BIDDER";
        RegisterPayload payload = new RegisterPayload(fullName, username, password, email, phone, roleStr);
        Packet packet = new Packet(PacketType.REGISTER, payload);

        // 3. GỬI GÓI TIN QUA MẠNG
        try {
            ServerConnection.getInstance().sendMessage(packet);
            System.out.println("[Client] Đã gửi yêu cầu đăng ký cho tài khoản: " + username);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", "Không thể kết nối đến máy chủ.");
        }
    }

    // 4. HÀM NÀY SẼ ĐƯỢC ServerHandler GỌI KHI NHẬN ĐƯỢC KẾT QUẢ TỪ SERVER
    public void handleSignupResult(boolean isSuccess, String message) {
        if (isSuccess) {
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đăng ký tài khoản thành công! Vui lòng đăng nhập.");
            goToLogin(currentEvent); // Chuyển về trang đăng nhập
        } else {
            showAlert(Alert.AlertType.ERROR, "Đăng ký thất bại", message);
        }
    }

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

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
