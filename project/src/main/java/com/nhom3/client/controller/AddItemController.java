package com.nhom3.client.controller;

import com.nhom3.client.utils.UserSession;
import com.nhom3.server.dao.ItemDAO;
import com.nhom3.server.dao.ItemDAOImpl;
import com.nhom3.server.service.ItemService;

import com.nhom3.shared.model.item.Item;
import com.nhom3.shared.model.user.Seller;
import com.nhom3.shared.model.user.User;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class AddItemController {

    @FXML private TextField txtName;
    @FXML private ComboBox<String> cbType;
    @FXML private TextField txtStartPrice;
    @FXML private Item editingItem = null;

    @FXML
    public void initialize() {
        // Đổ dữ liệu vào ComboBox (Phải khớp với chuỗi Type trong Database của bạn)
        cbType.setItems(FXCollections.observableArrayList("ART", "ELECTRONICS", "VEHICLE"));

        // Ép người dùng chỉ được nhập số vào ô Giá khởi điểm
        txtStartPrice.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                txtStartPrice.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
    }

    @FXML
    public void setEditingItem(Item item) {
        this.editingItem = item;
        // Đổ dữ liệu cũ lên các ô TextField
        txtName.setText(item.getName());
        txtStartPrice.setText(String.format("%.0f", item.getStartPrice()));   
        cbType.setValue(item.getType().name());
        // Khóa không cho đổi Loại (Vì đổi loại sẽ phải thay đổi class Creator rất phức tạp)
        cbType.setDisable(true); 
    }

    @FXML
    void handleSave(ActionEvent event) {
        String name = txtName.getText().trim();
        String type = cbType.getValue();
        String priceStr = txtStartPrice.getText().trim();
        // 1. Kiểm tra nhập liệu
        if (name.isEmpty() || type == null || priceStr.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng điền đầy đủ các trường!");
            return;
        }
        double startPrice = Double.parseDouble(priceStr);
        // 2. Lấy thông tin Seller hiện tại
        User currentUser = UserSession.getInstance().getLoggedInUser();
        if (!(currentUser instanceof Seller)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi quyền hạn", "Chỉ người bán (Seller) mới được thêm sản phẩm!");
            return;
        }
        Seller seller = (Seller) currentUser;
        // 3. Chia luồng xử lí
        // Chế đọ thêm mới
        if (editingItem == null) {
            com.nhom3.shared.model.item.ItemType typeEnum = com.nhom3.shared.model.item.ItemType.valueOf(type);
            // Tạo Item mới (ID = 0 để DB tự tăng)
            Item newItem = typeEnum.createItem(0, name, startPrice);
            ItemService itemService = new ItemService();
            boolean isSuccess = itemService.createItem(seller, newItem);
            if (isSuccess) {
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã thêm sản phẩm mới vào kho!");
                closeWindow();
            } else {
                showAlert(Alert.AlertType.ERROR, "Lỗi Database", "Không thể lưu sản phẩm!");
            }
        } else {
            // Chế đọ chỉnh sửa
            // Gán dữ liệu mới vào đối tượng hiện tại
            editingItem.setName(name);
            editingItem.setStartPrice(startPrice);
            editingItem.setCurHighest(startPrice); 
            ItemDAO itemDAO = new ItemDAOImpl();
            boolean isSuccess = itemDAO.updateItem(editingItem);
            if (isSuccess) {
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã cập nhật thông tin sản phẩm!");
                closeWindow();
            } else {
                showAlert(Alert.AlertType.ERROR, "Lỗi Database", "Không thể cập nhật sản phẩm!");
            }
        }
    }

    @FXML
    void handleCancel(ActionEvent event) {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) txtName.getScene().getWindow();
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
