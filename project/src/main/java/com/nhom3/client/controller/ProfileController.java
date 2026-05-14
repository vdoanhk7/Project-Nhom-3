package com.nhom3.client.controller;

import com.nhom3.client.network.ServerConnection;
import com.nhom3.client.utils.UserSession;
import com.nhom3.shared.model.user.Admin;
import com.nhom3.shared.model.user.Seller;
import com.nhom3.shared.model.user.User;
import com.nhom3.shared.network.packet.Packet;
import com.nhom3.shared.network.packet.PacketType;
import com.nhom3.shared.network.payload.ResultPayload;
import com.nhom3.shared.network.payload.UserProfilePayload;
import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;

@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
        value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD",
        justification = "JavaFX controllers are reached from the socket dispatcher through the active screen instance.")
public class ProfileController {
    private static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    private static final String PHONE_REGEX = "^0\\d{9}$";

    @FXML private TextField txtName;
    @FXML private TextField txtEmail;
    @FXML private TextField txtPhone;
    @FXML private TextField txtRole;
    @FXML private Button btnEditSave;
    @FXML private Button btnCancel;
    @FXML private Label lblUsername;

    private boolean editMode;
    private static ProfileController instance;

    public static ProfileController getInstance() {
        return instance;
    }

    @FXML
    public void initialize() {
        instance = this;
        loadUserToForm();
        txtPhone.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                txtPhone.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
        btnCancel.setVisible(false);
        btnCancel.setManaged(false);
    }

    @FXML
    void handleEditSave(ActionEvent event) {
        if (!editMode) {
            setFieldsEditable(true);
            btnEditSave.setText("LƯU THAY ĐỔI");
            btnCancel.setVisible(true);
            btnCancel.setManaged(true);
            editMode = true;
            return;
        }
        sendUpdateProfileRequest();
    }

    @FXML
    void handleCancel(ActionEvent event) {
        loadUserToForm();
        resetToViewMode();
    }

    @FXML
    void handleChangePassword(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/nhom3/client/view/change_password.fxml"));
            Parent root = loader.load();
            Stage popupStage = new Stage();
            popupStage.setTitle("Thay đổi mật khẩu");
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setScene(new Scene(root));
            popupStage.setResizable(false);
            popupStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống", "Không thể mở cửa sổ đổi mật khẩu!");
        }
    }

    public void handleUpdateProfileResult(ResultPayload result) {
        if (!result.getResult()) {
            showAlert(Alert.AlertType.ERROR, "Thất bại", result.getMessage());
            return;
        }

        User currentUser = UserSession.getInstance().getLoggedInUser();
        if (currentUser != null) {
            currentUser.getUserInfo().setName(result.getFullName());
            currentUser.getUserContact().setEmail(result.getEmail());
            currentUser.getUserContact().setPhoneNumber(result.getPhone());
        }
        resetToViewMode();
        showAlert(Alert.AlertType.INFORMATION, "Thành công", result.getMessage());
    }

    private void sendUpdateProfileRequest() {
        User currentUser = UserSession.getInstance().getLoggedInUser();
        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không tìm thấy phiên đăng nhập!");
            return;
        }

        String fullName = txtName.getText().trim();
        String email = txtEmail.getText().trim();
        String phone = txtPhone.getText().trim();

        if (fullName.isEmpty() || email.isEmpty() || phone.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng nhập đầy đủ họ tên, email và số điện thoại!");
            return;
        }

        if (!isValidEmail(email)) {
            showAlert(Alert.AlertType.ERROR, "Email không hợp lệ", "Email phải đúng định dạng hợp lệ.");
            focusField(txtEmail);
            return;
        }

        if (!isValidPhone(phone)) {
            showAlert(Alert.AlertType.ERROR, "Số điện thoại không hợp lệ", "Số điện thoại phải gồm đúng 10 số và bắt đầu bằng số 0");
            focusField(txtPhone);
            return;
        }

        try {
            UserProfilePayload payload = new UserProfilePayload(
                    currentUser.getId(),
                    currentUser.getUserInfo().getUserName(),
                    fullName,
                    currentUser.getRole().name(),
                    email,
                    phone);
            ServerConnection.getInstance().sendMessage(new Packet(PacketType.UPDATE_PROFILE, payload));
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi mạng", "Không thể gửi yêu cầu cập nhật!");
        }
    }

    private void loadUserToForm() {
        User currentUser = UserSession.getInstance().getLoggedInUser();
        if (currentUser == null) {
            return;
        }

        txtName.setText(currentUser.getUserInfo().getName());
        lblUsername.setText("@" + currentUser.getUserInfo().getUserName());
        if (currentUser.getUserContact() != null) {
            txtEmail.setText(currentUser.getUserContact().getEmail());
            txtPhone.setText(currentUser.getUserContact().getPhoneNumber());
        }
        if (currentUser instanceof Admin) {
            txtRole.setText("Admin");
        } else if (currentUser instanceof Seller) {
            txtRole.setText("Seller");
        } else {
            txtRole.setText("Bidder");
        }
    }

    private void setFieldsEditable(boolean value) {
        txtName.setEditable(value);
        txtEmail.setEditable(value);
        txtPhone.setEditable(value);
    }

    private void resetToViewMode() {
        setFieldsEditable(false);
        btnEditSave.setText("CHỈNH SỬA THÔNG TIN");
        btnCancel.setVisible(false);
        btnCancel.setManaged(false);
        editMode = false;
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches(EMAIL_REGEX);
    }

    private boolean isValidPhone(String phone) {
        return phone != null && phone.matches(PHONE_REGEX);
    }

    private void focusField(TextField field) {
        if (field == null) {
            return;
        }
        javafx.application.Platform.runLater(() -> {
            field.requestFocus();
            field.positionCaret(field.getText().length());
        });
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
