package com.nhom3.client.controller;

import com.nhom3.shared.model.item.Item;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class PublishAuctionController {
    @FXML private Label lblItemName;
    @FXML private DatePicker dpStartDate, dpEndDate;
    @FXML private TextField txtStartTime, txtEndTime;

    private Item currentItem;
    private static PublishAuctionController instance;
    @FXML public void initialize() { instance = this; }
    public static PublishAuctionController getInstance() { return instance; }

    public void setItem(Item item) {
        this.currentItem = item;
        lblItemName.setText(item.getName());
        dpStartDate.setValue(LocalDate.now());
        dpEndDate.setValue(LocalDate.now().plusDays(1)); // Mặc định kết thúc sau 1 ngày
    }

    @FXML
    void handleConfirm() {
        try {
            LocalDateTime start = LocalDateTime.of(dpStartDate.getValue(), LocalTime.parse(txtStartTime.getText()));
            LocalDateTime end = LocalDateTime.of(dpEndDate.getValue(), LocalTime.parse(txtEndTime.getText()));
            
            if (start.isBefore(LocalDateTime.now())) {
                showAlert("Lỗi", "Thời gian bắt đầu không được ở trong quá khứ!");
                return;
            }
            if (end.isBefore(start)) {
                showAlert("Lỗi", "Thời gian kết thúc phải sau thời gian bắt đầu!");
                return;
            }

            // --- BẮT ĐẦU GỬI MẠNG ---
            com.nhom3.shared.network.payload.PublishAuctionPayload payload = new com.nhom3.shared.network.payload.PublishAuctionPayload(
                currentItem.getId(), start.toString(), end.toString()
            );
            com.nhom3.shared.network.packet.Packet packet = new com.nhom3.shared.network.packet.Packet(com.nhom3.shared.network.packet.PacketType.PUBLISH_AUCTION, payload);
            
            com.nhom3.client.network.ServerConnection.getInstance().sendMessage(packet);
            System.out.println("[Client] Đã gửi yêu cầu đăng bán sản phẩm ID: " + currentItem.getId());

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Lỗi định dạng", "Vui lòng nhập giờ đúng định dạng HH:mm (VD: 08:30)");
        }
    }

    public void handlePublishResult(boolean isSuccess, String message) {
        if (isSuccess) {
            showAlert("Thành công", "Sản phẩm đã được lên lịch đấu giá thành công!");
            ((Stage) lblItemName.getScene().getWindow()).close();
            
            // Cập nhật lại bảng của Seller ngay lập tức
            if (com.nhom3.client.controller.ManageItemController.getInstance() != null) {
                com.nhom3.client.controller.ManageItemController.getInstance().loadSellerItems();
            }
        } else {
            showAlert("Lỗi", message);
        }
    }

    @FXML void handleCancel() { ((Stage) lblItemName.getScene().getWindow()).close(); }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}