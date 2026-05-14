package com.nhom3.client.controller;

import com.nhom3.client.network.ServerConnection;
import com.nhom3.client.utils.UserSession;
import com.nhom3.shared.model.item.Item;
import com.nhom3.shared.model.user.Seller;
import com.nhom3.shared.model.user.User;
import com.nhom3.shared.network.packet.Packet;
import com.nhom3.shared.network.packet.PacketType;
import com.nhom3.shared.network.payload.ItemPayload;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
        value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD",
        justification = "JavaFX controllers are reached from the socket dispatcher through the active screen instance.")
public class AddItemController {

    @FXML private TextField txtName;
    @FXML private ComboBox<String> cbType;
    @FXML private TextField txtStartPrice;

    private Item editingItem;
    private static AddItemController instance;

    public static AddItemController getInstance() {
        return instance;
    }

    @FXML
    public void initialize() {
        instance = this;
        cbType.setItems(FXCollections.observableArrayList("ART", "ELECTRONICS", "VEHICLE"));
        txtStartPrice.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                txtStartPrice.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
    }

    public void setEditingItem(Item item) {
        editingItem = item;
        txtName.setText(item.getName());
        txtStartPrice.setText(String.format("%.0f", item.getStartPrice()));
        cbType.setValue(item.getType().name());
        cbType.setDisable(true);
    }

    @FXML
    void handleSave(ActionEvent event) {
        String name = txtName.getText().trim();
        String type = cbType.getValue();
        String priceText = txtStartPrice.getText().trim();

        if (name.isEmpty() || type == null || priceText.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Thieu thong tin",
                    "Vui long dien day du cac truong!");
            return;
        }

        User currentUser = UserSession.getInstance().getLoggedInUser();
        if (!(currentUser instanceof Seller seller)) {
            showAlert(Alert.AlertType.ERROR, "Loi quyen han",
                    "Chi nguoi ban (Seller) moi duoc them san pham!");
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
