package com.nhom3.client.controller;

import com.nhom3.client.network.ServerConnection;
import com.nhom3.client.utils.UserSession;
import com.nhom3.shared.model.user.User;
import com.nhom3.shared.network.packet.Packet;
import com.nhom3.shared.network.packet.PacketType;
import com.nhom3.shared.network.payload.ChangePasswordPayload;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;

@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
        value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD",
        justification = "JavaFX controllers are reached from the socket dispatcher through the active screen instance.")
public class ChangePasswordController {

    @FXML private PasswordField txtOldPass;
    @FXML private PasswordField txtNewPass;
    @FXML private PasswordField txtConfirmPass;
    @FXML private Button btnCancel;

    private static ChangePasswordController instance;

    public static ChangePasswordController getInstance() {
        return instance;
    }

    @FXML
    public void initialize() {
        instance = this;
    }

    @FXML
    void handleSavePassword(ActionEvent event) {
        String oldPass = txtOldPass.getText();
        String newPass = txtNewPass.getText();
        String confirmPass = txtConfirmPass.getText();

        if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Canh bao", "Vui long nhap day du thong tin!");
            return;
        }

        if (!newPass.equals(confirmPass)) {
            showAlert(Alert.AlertType.ERROR, "Loi nhap lieu", "Mat khau xac nhan khong khop!");
            return;
        }

        if (newPass.equals(oldPass)) {
            showAlert(Alert.AlertType.WARNING, "Thong bao", "Mat khau moi phai khac mat khau cu!");
            return;
        }

        User currentUser = UserSession.getInstance().getLoggedInUser();
        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Loi", "Khong tim thay phien dang nhap!");
            return;
        }

        try {
            ChangePasswordPayload payload =
                    new ChangePasswordPayload(currentUser.getId(), oldPass, newPass);
            ServerConnection.getInstance().sendMessage(new Packet(PacketType.CHANGE_PASSWORD, payload));
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Loi mang", "Khong the gui yeu cau doi mat khau!");
        }
    }

    public void handleChangePasswordResult(boolean success, String message) {
        showAlert(success ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR,
                success ? "Thanh cong" : "That bai", message);
        if (success) {
            closeWindow();
        }
    }

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
