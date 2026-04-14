package com.nhom3.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class ProfileController {

    // Khai báo các thành phần đã gắn fx:id bên FXML
    @FXML private TextField txtName;
    @FXML private TextField txtEmail;
    @FXML private TextField txtPhone;
    @FXML private TextField txtRole;
    @FXML private Button btnEditSave;
    @FXML private Button btnCancel;

    // Biến lưu trạng thái hiện tại (Đang xem hay đang sửa)
    private boolean isEditMode = false;

    @FXML
    public void initialize() {
        // Hàm này chạy ngay khi màn hình Profile vừa được load lên
        // Ấn nút "Hủy" và làm nó biến mất hoàn toàn khỏi bố cục
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
            alert.setContentText("Đã cập nhật thông tin cá nhân!");
            alert.showAndWait();
        }
    }

    @FXML
    void handleCancel(ActionEvent event) {
        // Nếu đang sửa mà đổi ý, bấm Hủy sẽ quay về như cũ
        resetToViewMode();
        // (Ghi chú: Nơi đây sau này sẽ viết code để nạp lại dữ liệu cũ từ Database 
        // để xóa đi những chữ mà người dùng vừa gõ nháp)
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
        
        // Đổi lại giao diện nút bấm thành màu xanh dương (Sửa)
        btnEditSave.setText("CHỈNH SỬA THÔNG TIN");
        btnEditSave.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand;");
        
        // Ẩn nút Hủy
        btnCancel.setVisible(false);
        btnCancel.setManaged(false);
        
        isEditMode = false;
    }

    private void saveDataToDatabase() {
        System.out.println("Đang lưu tên mới: " + txtName.getText());
        System.out.println("Đang lưu sđt mới: " + txtPhone.getText());
        // Sau này gọi Server/Database ở đây
    }
}