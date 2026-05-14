package com.nhom3.client.controller;

import com.nhom3.client.network.ServerConnection;
import com.nhom3.client.utils.MoneyInputFormatter;
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
        MoneyInputFormatter.install(txtStartPrice);
    }

    public void setEditingItem(Item item) {
        editingItem = item;
        txtName.setText(item.getName());
        txtStartPrice.setText(MoneyInputFormatter.formatAmount(item.getStartPrice()));
        cbType.setValue(item.getType().name());
        cbType.setDisable(true);
    }

    @FXML
    void handleSave(ActionEvent event) {
        String name = txtName.getText().trim();
        String type = cbType.getValue();
        String priceText = txtStartPrice.getText().trim();

        if (name.isEmpty() || type == null || priceText.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng điền đầy đủ các trường!");
            return;
        }

        double startPriceVal;
        try {
            startPriceVal = MoneyInputFormatter.parseAmount(priceText);
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi định dạng", "Giá khởi điểm phải là một số hợp lệ (VD: 50000 hoặc 50.000)!");
            return;
        }

        User currentUser = UserSession.getInstance().getLoggedInUser();
        if (!(currentUser instanceof Seller)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi quyền hạn", "Chỉ người bán (Seller) mới được thêm sản phẩm!");
            return;
        }
        Seller seller = (Seller) currentUser;

        try {
            ItemPayload payload = new ItemPayload(
                    editingItem == null ? 0 : editingItem.getId(),
                    seller.getId(),
                    name,
                    type,
                    startPriceVal
            );
            PacketType packetType = editingItem == null ? PacketType.SAVE_ITEM : PacketType.UPDATE_ITEM;
            Packet packet = new Packet(packetType, payload);
            
            ServerConnection.getInstance().sendMessage(packet);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi mạng", "Không thể kết nối đến máy chủ để lưu sản phẩm!");
        }
    }

    public void handleItemMutationResult(PacketType type, boolean success, String message) {
        javafx.application.Platform.runLater(() -> {
            showAlert(success ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR, 
                     success ? "Thành công" : "Lỗi Server", message);
            if (success) {
                closeWindow();
                if (ManageItemController.getInstance() != null) {
                    ManageItemController.getInstance().loadSellerItems();
                }
            }
        });
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
