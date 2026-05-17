package com.nhom3.client.controller;

import com.nhom3.client.event.ClientEventBus;
import com.nhom3.client.event.ClientEvents;
import com.nhom3.client.event.ControllerLifecycle;
import com.nhom3.client.network.ServerConnection;
import com.nhom3.client.utils.MoneyInputFormatter;
import com.nhom3.client.utils.UserSession;
import com.nhom3.shared.model.item.Item;
import com.nhom3.shared.model.item.ItemType;
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
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.util.Base64;

public class AddItemController {

    @FXML private TextField txtName;
    @FXML private TextArea txtDescription;
    @FXML private ComboBox<String> cbType;
    @FXML private TextField txtStartPrice;
    @FXML private ImageView imgPreview;
    @FXML private Label lblImageName;

    private String selectedImageBase64 = null;

    private Item editingItem;
    private boolean waitingForSaveResult;

    @FXML
    public void initialize() {
        ClientEventBus.getDefault().subscribe(
                ClientEvents.ItemMutationResult.class, this, AddItemController::handleItemMutationEvent);
        ControllerLifecycle.unsubscribeOnDetach(txtName, this);
        cbType.setItems(FXCollections.observableArrayList(
                java.util.Arrays.stream(ItemType.values()).map(Enum::name).toList()));
        MoneyInputFormatter.install(txtStartPrice);
    }

    public void setEditingItem(Item item) {
        editingItem = item;
        txtName.setText(item.getName());
        txtDescription.setText(item.getDescription() != null ? item.getDescription() : "");
        txtStartPrice.setText(MoneyInputFormatter.formatAmount(item.getStartPrice()));
        cbType.setValue(item.getType().name());
        cbType.setDisable(true);
        if (item.getImageBase64() != null && !item.getImageBase64().isEmpty()) {
            this.selectedImageBase64 = item.getImageBase64();
            byte[] imageBytes = Base64.getDecoder().decode(this.selectedImageBase64);
            imgPreview.setImage(new Image(new java.io.ByteArrayInputStream(imageBytes)));
            lblImageName.setText("Đã có ảnh");
        }
    }

    @FXML
    void handleChooseImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn Ảnh Sản Phẩm");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        Stage stage = (Stage) txtName.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            try {
                selectedImageBase64 = com.nhom3.client.utils.ImageUtils.compressAndEncodeImage(file);
                if (selectedImageBase64 == null) {
                    throw new Exception("Không thể xử lý ảnh.");
                }
                imgPreview.setImage(new Image(file.toURI().toString()));
                lblImageName.setText(file.getName());
            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Lỗi đọc file", "Không thể đọc file ảnh đã chọn!");
            }
        }
    }

    @FXML
    void handleSave(ActionEvent event) {
        String name = txtName.getText().trim();
        String description = txtDescription.getText().trim();
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
                    description,
                    type,
                    startPriceVal,
                    selectedImageBase64
            );
            PacketType packetType = editingItem == null ? PacketType.SAVE_ITEM : PacketType.UPDATE_ITEM;
            Packet packet = new Packet(packetType, payload);
            
            waitingForSaveResult = true;
            ServerConnection.getInstance().sendMessage(packet);
        } catch (Exception e) {
            waitingForSaveResult = false;
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi mạng", "Không thể kết nối đến máy chủ để lưu sản phẩm!");
        }
    }

    public void handleItemMutationResult(PacketType type, boolean success, String message) {
        if (!waitingForSaveResult) {
            return;
        }
        waitingForSaveResult = false;

        javafx.application.Platform.runLater(() -> {
            showAlert(success ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR, 
                     success ? "Thành công" : "Lỗi Server", message);
            if (success) {
                closeWindow();
                ClientEventBus.getDefault().publish(new ClientEvents.SellerItemsChanged());
            }
        });
    }

    private void handleItemMutationEvent(ClientEvents.ItemMutationResult event) {
        handleItemMutationResult(event.packetType(), event.success(), event.message());
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
