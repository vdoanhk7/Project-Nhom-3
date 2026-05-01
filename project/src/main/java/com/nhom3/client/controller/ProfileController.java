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
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;
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
            txtName.setText(currentUser.getUserInfo().getName());
            lblUsername.setText("@" + currentUser.getUserInfo().getUserName());
            if (currentUser.getUserContact() != null) {
                txtEmail.setText(currentUser.getUserContact().getEmail());
                txtPhone.setText(currentUser.getUserContact().getPhoneNumber());
            } else {
                txtEmail.setText("Chưa cập nhật");
                txtPhone.setText("Chưa cập nhật");
            }
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

    @FXML
    void handleChangePassword(ActionEvent event) {
        try {
            // 1. Chỉ định đường dẫn tới file FXML của cửa sổ đổi mật khẩu
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/nhom3/client/view/change_password.fxml"));
            Parent root = loader.load();
            // 2. Tạo một cửa sổ (Stage) mới cho Pop-up
            Stage popupStage = new Stage();
            popupStage.setTitle("Thay đổi mật khẩu bảo mật");    
            // 3. Thiết lập chế độ MODAL cho cửa sổ này (Ngăn không cho tương tác với cửa sổ chính khi Pop-up đang mở)
            popupStage.initModality(Modality.APPLICATION_MODAL);          
            // 4. Hiển thị cửa sổ
            popupStage.setScene(new Scene(root));
            popupStage.setResizable(false); // Không cho phép phóng to cửa sổ
            popupStage.showAndWait(); // Đợi cho đến khi cửa sổ này đóng lại mới thực hiện tiếp
        
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Lỗi: Không tìm thấy file change_password.fxml tại đường dẫn đã chỉ định.");
            
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi hệ thống");
            alert.setHeaderText(null);
            alert.setContentText("Không thể mở cửa sổ đổi mật khẩu. Vui lòng thử lại sau!");
            alert.showAndWait();
        }
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