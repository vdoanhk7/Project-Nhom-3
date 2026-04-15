package com.nhom3.client.controller;

import com.nhom3.client.utils.UserSession;
import com.nhom3.server.service.AuthService;
import com.nhom3.shared.model.user.User;
import com.nhom3.shared.model.user.Admin;
import com.nhom3.shared.model.user.Seller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
public class ProfileController {

    // Khai báo các thành phần đã gắn fx:id bên FXML
    @FXML private TextField txtName;
    @FXML private TextField txtEmail;
    @FXML private TextField txtPhone;
    @FXML private TextField txtRole;
    @FXML private Button btnEditSave;
    @FXML private Button btnCancel;
    @FXML private Label lblUsername;

    // Biến lưu trạng thái hiện tại (Đang xem hay đang sửa)
    private boolean isEditMode = false;

    // Hàm chạy ngay khi giao diện vừa load lên
    @FXML
    public void initialize() {
        // 1. Lấy dữ liệu người dùng từ "Ví" UserSession
        User currentUser = UserSession.getInstance().getLoggedInUser();
        if (currentUser != null) {
            // 2. Đổ dữ liệu vào các ô TextField
            txtName.setText(currentUser.getUserInfo().getName());
            txtEmail.setText(currentUser.getUserContact().getEmail());
            txtPhone.setText(currentUser.getUserContact().getPhoneNumber());
            lblUsername.setText("@" + currentUser.getUserInfo().getUserName());
            // Đổ dữ liệu vai trò (Sử dụng instanceof để hiển thị tiếng Việt cho đẹp)
            if (currentUser instanceof Admin) {
                txtRole.setText("Quản trị viên (Admin)");
            } else if (currentUser instanceof Seller) {
                txtRole.setText("Người bán (Seller)");
            } else {
                txtRole.setText("Người mua (Bidder)");
            }
        }
        // 3. Thiết lập trạng thái ban đầu cho các nút
        btnCancel.setVisible(false);
        btnCancel.setManaged(false);
    }

    @FXML
    void handleEditSave(ActionEvent event) {
        if (!isEditMode) {
            // NẾU ĐANG LÀ CHẾ ĐỘ XEM -> BẤM VÀO ĐỂ SỬA
            setFieldsEditable(true);
            
            // Đổi giao diện nút bấm sang màu xanh lá (Lưu)
            btnEditSave.setText("LƯU THAY ĐỔI");
            btnEditSave.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand;");
            
            // Hiện nút Hủy
            btnCancel.setVisible(true);
            btnCancel.setManaged(true);
            
            isEditMode = true;
        } else {
            // NẾU ĐANG LÀ CHẾ ĐỘ SỬA -> BẤM VÀO ĐỂ LƯU
            saveDataToDatabase();
            
            // Quay về trạng thái chỉ xem
            resetToViewMode();
            
            // Hiện thông báo thành công
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Thành công");
            alert.setHeaderText(null);
            alert.setContentText("Cập nhật thông tin cá nhân thành công!");
            alert.showAndWait();
        }
    }

    @FXML
    void handleCancel(ActionEvent event) {
        // Nếu đang sửa mà đổi ý, bấm Hủy sẽ quay về như cũ
        resetToViewMode();
        User currentUser = UserSession.getInstance().getLoggedInUser();
        if (currentUser != null) {
            txtName.setText(currentUser.getUserInfo().getName());
            txtEmail.setText(currentUser.getUserContact().getEmail());
            txtPhone.setText(currentUser.getUserContact().getPhoneNumber());
        }
        System.out.println("Đã huỷ bỏ chỉnh sửa thông tin cá nhân");
    }

    // --- CÁC HÀM HỖ TRỢ ---

    private void setFieldsEditable(boolean value) {
        txtName.setEditable(value);
        txtEmail.setEditable(value);
        txtPhone.setEditable(value);
        
        // Đổi màu nền: Nếu đang sửa thì nền trắng/viền xanh, Nếu khóa thì nền xám
        String textFieldStyle = value 
            ? "-fx-padding: 8; -fx-background-color: #ffffff; -fx-border-color: #3498db; -fx-border-radius: 5;" 
            : "-fx-padding: 8; -fx-background-color: #f8f9fa; -fx-border-color: #e0e0e0; -fx-border-radius: 5;";

        txtName.setStyle(textFieldStyle);
        txtEmail.setStyle(textFieldStyle);
        txtPhone.setStyle(textFieldStyle);
    }

    private void resetToViewMode() {
        setFieldsEditable(false);

        btnEditSave.setText("CHỈNH SỬA THÔNG TIN");
        btnEditSave.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand;");
        
        // Ẩn nút Hủy
        btnCancel.setVisible(false);
        btnCancel.setManaged(false);
        
        isEditMode = false;
    }

    private void saveDataToDatabase() {
        // 1. Lấy đối tượng người dùng hiện tại từ Session
        User currentUser = UserSession.getInstance().getLoggedInUser();

        if (currentUser != null) {
            // 2. Lấy dữ liệu mới từ giao diện (các ô TextField)
            String newName = txtName.getText().trim();
            String newEmail = txtEmail.getText().trim();
            String newPhone = txtPhone.getText().trim();

            // 3. Cập nhật thông tin vào đối tượng currentUser
            currentUser.getUserInfo().setName(newName);
            currentUser.getUserContact().setEmail(newEmail);
            currentUser.getUserContact().setPhoneNumber(newPhone);

            // 4. Gọi Service để đẩy dữ liệu xuống Database
            AuthService authService = new AuthService();
            
            boolean success = authService.updateUser(currentUser);

            if (success) {
                System.out.println("Lưu Database thành công!");
            } else {
                System.out.println("Lưu Database thất bại!");
            }
        }
    }
}