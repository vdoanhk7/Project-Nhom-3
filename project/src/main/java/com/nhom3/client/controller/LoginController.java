package com.nhom3.client.controller;

import com.nhom3.client.network.ServerConnection;
import com.nhom3.client.utils.UserSession;
import com.nhom3.shared.model.user.User;
import com.nhom3.shared.network.packet.Packet;
import com.nhom3.shared.network.packet.PacketType;
import com.nhom3.shared.network.payload.LoginPayload;
import javafx.scene.Node;
import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
        value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD",
        justification = "JavaFX controllers are reached from the socket dispatcher through the active screen instance.")
public class LoginController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;

    // Lưu lại instance hiện tại để luồng mạng (ServerHandler) có thể gọi tới
    private static LoginController instance;
    private Stage currentStage; // Lưu lại màn hình hiện tại để chuyển trang

    @FXML
    public void initialize() {
        instance = this; // Gán thể hiện hiện tại
    }

    public static LoginController getInstance() {
        return instance;
    }

    @FXML
    void handleLogin(ActionEvent event) {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng nhập đầy đủ tài khoản và mật khẩu!");
            return;
        }

        // LƯU LẠI STAGE ĐỂ DÙNG CHUYỂN TRANG SAU NÀY
        currentStage = (Stage) txtUsername.getScene().getWindow();

        // THAY VÌ GỌI AuthService -> GỬI PACKET QUA MẠNG
        try {
            LoginPayload payload = new LoginPayload(username, password);
            Packet packet = new Packet(PacketType.LOGIN, payload);
            
            ServerConnection.getInstance().sendMessage(packet);
            System.out.println("[Client] Đã gửi yêu cầu đăng nhập...");
            
            // Tạm thời vô hiệu hóa nút bấm/hiện loading ở đây (tùy bạn phát triển thêm)
            
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi mạng", "Không thể kết nối đến Server!");
        }
    }

    // Luồng mạng sẽ gọi hàm này khi có kết quả từ Server
    public void handleLoginResult(boolean isSuccess, User user) {
        if (isSuccess && user != null) {
            UserSession.getInstance().setLoggedInUser(user);
            System.out.println(">> Đăng nhập thành công: " + user.getUserInfo().getName());
            goToMainLayout();
        } else {
            showAlert(Alert.AlertType.ERROR, "Thất bại", "Sai tên đăng nhập hoặc mật khẩu!");
        }
    }

    private void goToMainLayout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/nhom3/client/view/layout_main.fxml"));
            Parent root = loader.load();
            currentStage.setScene(new Scene(root, 1300, 800));
            currentStage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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
