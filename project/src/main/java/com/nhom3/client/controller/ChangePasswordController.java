package com.nhom3.client.controller;

import com.nhom3.client.event.ClientEventBus;
import com.nhom3.client.event.ClientEvents;
import com.nhom3.client.event.ControllerLifecycle;
import com.nhom3.client.network.ServerConnection;
import com.nhom3.client.utils.DialogUtils;
import com.nhom3.client.utils.UserSession;
import com.nhom3.shared.model.user.User;
import com.nhom3.shared.network.packet.Packet;
import com.nhom3.shared.network.packet.PacketType;
import com.nhom3.shared.network.payload.ChangePasswordPayload;
import javafx.application.Platform;
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
    @FXML private Button btnSave;

    private boolean waitingForChangePasswordResult;

    @FXML
    public void initialize() {
        ClientEventBus.getDefault().subscribe(
                ClientEvents.ChangePasswordResult.class, this, ChangePasswordController::handleChangePasswordEvent);
        ControllerLifecycle.unsubscribeOnDetach(btnCancel, this);
    }

    @FXML
    void handleSavePassword(ActionEvent event) {
        if (waitingForChangePasswordResult) {
            return;
        }

        String oldPass = txtOldPass.getText();
        String newPass = txtNewPass.getText();
        String confirmPass = txtConfirmPass.getText();

        if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        if (!newPass.equals(confirmPass)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi nhập liệu", "Mật khẩu xác nhận không khớp!");
            return;
        }

        if (newPass.equals(oldPass)) {
            showAlert(Alert.AlertType.WARNING, "Thông báo", "Mật khẩu mới phải khác mật khẩu cũ!");
            return;
        }

        User currentUser = UserSession.getInstance().getLoggedInUser();
        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không tìm thấy phiên đăng nhập!");
            return;
        }

        try {
            ChangePasswordPayload payload =
                    new ChangePasswordPayload(currentUser.getId(), oldPass, newPass);
            setChangePasswordPending(true);
            ServerConnection.getInstance().sendMessage(new Packet(PacketType.CHANGE_PASSWORD, payload));
        } catch (Exception e) {
            setChangePasswordPending(false);
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi mạng", "Không thể gửi yêu cầu đổi mật khẩu!");
        }
    }

    public void handleChangePasswordResult(boolean success, String message) {
        if (!waitingForChangePasswordResult) {
            return;
        }
        setChangePasswordPending(false);

        Platform.runLater(() -> {
            if (success) {
                DialogUtils.showAlertAsync(Alert.AlertType.INFORMATION, "Thành công", message, btnCancel, this::closeWindow);
            } else {
                showAlert(Alert.AlertType.ERROR, "Thất bại", message);
            }
        });
    }

    private void handleChangePasswordEvent(ClientEvents.ChangePasswordResult event) {
        handleChangePasswordResult(event.success(), event.message());
    }

    @FXML
    void handleCancel(ActionEvent event) {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }

    private void setChangePasswordPending(boolean pending) {
        waitingForChangePasswordResult = pending;
        Runnable updateButtons = () -> {
            if (btnSave != null) {
                btnSave.setDisable(pending);
            }
            if (btnCancel != null) {
                btnCancel.setDisable(pending);
            }
        };
        if (Platform.isFxApplicationThread()) {
            updateButtons.run();
        } else {
            Platform.runLater(updateButtons);
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        DialogUtils.showAlertAsync(type, title, content, btnCancel);
    }
}
