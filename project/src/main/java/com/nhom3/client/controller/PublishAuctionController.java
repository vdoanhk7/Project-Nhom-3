package com.nhom3.client.controller;

import com.nhom3.shared.model.item.Item;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
        value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD",
        justification = "JavaFX controllers are reached from the socket dispatcher through the active screen instance.")
public class PublishAuctionController {
    @FXML private Label lblItemName;
    @FXML private RadioButton rbPublishNow;
    @FXML private RadioButton rbSchedule;
    @FXML private DatePicker dpStartDate, dpEndDate;
    @FXML private TextField txtStartTime, txtEndTime;

    private Item currentItem;
    private static PublishAuctionController instance;
    private final ToggleGroup publishModeGroup = new ToggleGroup();
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    public void initialize() {
        instance = this;
        rbPublishNow.setToggleGroup(publishModeGroup);
        rbSchedule.setToggleGroup(publishModeGroup);
        rbSchedule.setSelected(true);
        publishModeGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> updatePublishModeUI());
        updatePublishModeUI();
    }

    public static PublishAuctionController getInstance() { return instance; }

    public void setItem(Item item) {
        this.currentItem = item;
        lblItemName.setText(item.getName());
        dpStartDate.setValue(LocalDate.now());
        dpEndDate.setValue(LocalDate.now().plusDays(1)); // Mặc định kết thúc sau 1 ngày
        txtStartTime.setText("08:00");
        txtEndTime.setText("22:00");
        updatePublishModeUI();
    }

    @FXML
    void handleConfirm() {
        try {
            LocalDateTime start;
            if (isPublishNowMode()) {
                start = LocalDateTime.now();
            } else {
                start = LocalDateTime.of(dpStartDate.getValue(), LocalTime.parse(txtStartTime.getText()));
                if (start.isBefore(LocalDateTime.now())) {
                    showAlert("Lỗi", "Thời gian bắt đầu không được ở trong quá khứ!");
                    return;
                }
            }
            LocalDateTime end = LocalDateTime.of(dpEndDate.getValue(), LocalTime.parse(txtEndTime.getText()));
            
            if (!end.isAfter(start)) {
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
            showAlert("Thành công", isPublishNowMode()
                ? "Sản phẩm đã được đăng bán ngay thành công!"
                : "Sản phẩm đã được lên lịch đấu giá thành công!");
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

    private boolean isPublishNowMode() {
        return rbPublishNow.isSelected();
    }

    private void updatePublishModeUI() {
        boolean isScheduleMode = rbSchedule.isSelected();
        dpStartDate.setDisable(!isScheduleMode);
        txtStartTime.setDisable(!isScheduleMode);

        if (!isScheduleMode) {
            LocalDateTime now = LocalDateTime.now();
            dpStartDate.setValue(now.toLocalDate());
            txtStartTime.setText(now.format(TIME_FORMATTER));
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
