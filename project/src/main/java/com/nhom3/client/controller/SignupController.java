package com.nhom3.client.controller;

import javafx.animation.PauseTransition;
import com.nhom3.client.event.ClientEventBus;
import com.nhom3.client.event.ClientEvents;
import com.nhom3.client.event.ControllerLifecycle;
import com.nhom3.client.network.ServerConnection;
import com.nhom3.shared.network.packet.Packet;
import com.nhom3.shared.network.packet.PacketType;
import com.nhom3.shared.network.payload.RegisterPayload;
import com.nhom3.shared.validation.PasswordValidator;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.stage.Stage;
import javafx.util.Duration;

public class SignupController {
    private static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    private static final String PHONE_REGEX = "^0\\d{9}$";

    @FXML private TextField txtFullName;
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private TextField txtEmail;
    @FXML private TextField txtPhone;
    @FXML private RadioButton radioBidder;
    @FXML private RadioButton radioSeller;
    @FXML private Label lblMessage;

    private ActionEvent currentEvent; // Lưu lại sự kiện click để lát chuyển trang

    @FXML
    public void initialize() {
        ClientEventBus.getDefault().subscribe(
                ClientEvents.SignupResult.class, this, SignupController::handleSignupEvent);
        ControllerLifecycle.unsubscribeOnDetach(txtUsername, this);
        clearMessage();
        txtPhone.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                txtPhone.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
        txtFullName.textProperty().addListener((observable, oldValue, newValue) -> clearMessage());
        txtUsername.textProperty().addListener((observable, oldValue, newValue) -> clearMessage());
        txtPassword.textProperty().addListener((observable, oldValue, newValue) -> clearMessage());
        txtConfirmPassword.textProperty().addListener((observable, oldValue, newValue) -> clearMessage());
        txtEmail.textProperty().addListener((observable, oldValue, newValue) -> clearMessage());
        txtPhone.textProperty().addListener((observable, oldValue, newValue) -> clearMessage());
    }

    @FXML
    void handleSignup(ActionEvent event) {
        String fullName = txtFullName.getText();
        String username = txtUsername.getText();
        String password = txtPassword.getText();
        String confirmPass = txtConfirmPassword.getText();
        String email = txtEmail.getText().trim();
        String phone = txtPhone.getText().trim();

        if (username.isEmpty() || password.isEmpty() || confirmPass.isEmpty() || fullName.isEmpty() || email.isEmpty() || phone.isEmpty()) {
            showMessage("Vui lòng nhập đầy đủ thông tin!", false);
            return;
        }

        if (!isValidEmail(email)) {
            showMessage("Email phải đúng định dạng hợp lệ.", false);
            focusField(txtEmail);
            return;
        }

        if (!isValidPhone(phone)) {
            showMessage("Số điện thoại phải gồm đúng 10 số và bắt đầu bằng số 0", false);
            focusField(txtPhone);
            return;
        }

        if (!password.equals(confirmPass)) {
            showMessage("Mật khẩu xác nhận không khớp!", false);
            return;
        }

        if (!PasswordValidator.isStrong(password)) {
            showMessage(PasswordValidator.STRONG_PASSWORD_MESSAGE, false);
            focusField(txtPassword);
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
            showMessage("Không thể kết nối đến máy chủ.", false);
        }
    }

    // 4. HÀM NÀY SẼ ĐƯỢC ServerHandler GỌI KHI NHẬN ĐƯỢC KẾT QUẢ TỪ SERVER
    public void handleSignupResult(boolean isSuccess, String message) {
        if (isSuccess) {
            showMessage("Đăng ký tài khoản thành công! Đang chuyển sang trang đăng nhập...", true);
            PauseTransition pause = new PauseTransition(Duration.seconds(1));
            pause.setOnFinished(event -> goToLogin(currentEvent));
            pause.play();
        } else {
            showMessage(message, false);
        }
    }

    private void handleSignupEvent(ClientEvents.SignupResult event) {
        handleSignupResult(event.success(), event.message());
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

    private void showMessage(String message, boolean success) {
        if (lblMessage == null) {
            return;
        }
        lblMessage.setText(message);
        lblMessage.setStyle(success ? "-fx-text-fill: #27ae60; -fx-font-weight: bold;" : "-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        lblMessage.setVisible(true);
        lblMessage.setManaged(true);
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches(EMAIL_REGEX);
    }

    private boolean isValidPhone(String phone) {
        return phone != null && phone.matches(PHONE_REGEX);
    }

    private void focusField(TextInputControl field) {
        if (field == null) {
            return;
        }
        javafx.application.Platform.runLater(() -> {
            field.requestFocus();
            field.positionCaret(field.getText().length());
        });
    }

    private void clearMessage() {
        if (lblMessage == null) {
            return;
        }
        lblMessage.setText("");
        lblMessage.setVisible(false);
        lblMessage.setManaged(false);
    }
}
