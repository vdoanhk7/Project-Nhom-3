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

        try {
            int itemId = editingItem == null ? 0 : editingItem.getId();
            double startPrice = Double.parseDouble(priceText);
            PacketType packetType = editingItem == null ? PacketType.SAVE_ITEM : PacketType.UPDATE_ITEM;
            ItemPayload payload = new ItemPayload(itemId, seller.getId(), name, type, startPrice);
            ServerConnection.getInstance().sendMessage(new Packet(packetType, payload));
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Loi nhap lieu", "Gia khoi diem phai la so!");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Loi mang", "Khong the gui yeu cau luu san pham!");
        }
    }

    public void handleItemMutationResult(PacketType type, boolean success, String message) {
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
