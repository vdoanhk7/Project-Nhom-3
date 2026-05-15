package com.nhom3.client.controller;

import com.nhom3.shared.model.item.Item;
import com.nhom3.client.utils.MoneyInputFormatter;
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
    @FXML private TextField txtBidStep;

    // Khai báo thêm 2 nút để cấu hình hiệu ứng Hover ở initialize
    @FXML private Button btnCancel;
    @FXML private Button btnConfirm;

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
        MoneyInputFormatter.install(txtBidStep);
        
        setupButtonStyles(); // Gọi hàm làm đẹp nút bấm
        updatePublishModeUI();
    }

    public static PublishAuctionController getInstance() { return instance; }

    // HÀM MỚI: Trang trí hiệu ứng Hover chuyên nghiệp cho 2 nút bấm
    private void setupButtonStyles() {
        if (btnConfirm != null) {
            String confirmDefault = "-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6; -fx-padding: 10 24; -fx-effect: dropshadow(three-pass-box, rgba(59, 130, 246, 0.4), 10, 0, 0, 4);";
            String confirmHover = "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6; -fx-padding: 10 24; -fx-effect: dropshadow(three-pass-box, rgba(59, 130, 246, 0.6), 12, 0, 0, 6);";
            btnConfirm.setStyle(confirmDefault);
            btnConfirm.setOnMouseEntered(e -> btnConfirm.setStyle(confirmHover));
            btnConfirm.setOnMouseExited(e -> btnConfirm.setStyle(confirmDefault));
        }

        if (btnCancel != null) {
            String cancelDefault = "-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6; -fx-border-color: #cbd5e1; -fx-border-radius: 6; -fx-padding: 9 20;";
            String cancelHover = "-fx-background-color: #e2e8f0; -fx-text-fill: #0f172a; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6; -fx-border-color: #94a3b8; -fx-border-radius: 6; -fx-padding: 9 20;";
            btnCancel.setStyle(cancelDefault);
            btnCancel.setOnMouseEntered(e -> btnCancel.setStyle(cancelHover));
            btnCancel.setOnMouseExited(e -> btnCancel.setStyle(cancelDefault));
        }
    }

    public void setItem(Item item) {
        this.currentItem = item;
        lblItemName.setText(item.getName());
        dpStartDate.setValue(LocalDate.now());
        dpEndDate.setValue(LocalDate.now().plusDays(1)); // Mặc định kết thúc sau 1 ngày
        txtStartTime.setText("08:00");
        txtEndTime.setText("22:00");
        // Kiểm tra xem class Auction có hằng số DEFAULT_BID_STEP không, nếu có lỗi đoạn này bạn thay bằng 50000 nhé
        try {
            txtBidStep.setText(MoneyInputFormatter.formatAmount(com.nhom3.shared.model.auction.Auction.DEFAULT_BID_STEP));
        } catch (Exception e) {
            txtBidStep.setText("50,000");
        }
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

            double bidStep = MoneyInputFormatter.parseAmount(txtBidStep.getText());
            if (bidStep <= 0) {
                showAlert("Lỗi", "Bước giá phải lớn hơn 0!");
                return;
            }

            // --- BẮT ĐẦU GỬI MẠNG ---
            com.nhom3.shared.network.payload.PublishAuctionPayload payload = new com.nhom3.shared.network.payload.PublishAuctionPayload(
                currentItem.getId(), start.toString(), end.toString(), bidStep
            );
            com.nhom3.shared.network.packet.Packet packet = new com.nhom3.shared.network.packet.Packet(com.nhom3.shared.network.packet.PacketType.PUBLISH_AUCTION, payload);
            
            com.nhom3.client.network.ServerConnection.getInstance().sendMessage(packet);
            System.out.println("[Client] Đã gửi yêu cầu đăng bán sản phẩm ID: " + currentItem.getId());

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Lỗi định dạng", "Vui lòng nhập giờ đúng định dạng HH:mm và bước giá hợp lệ.");
        }
    }

    public void handlePublishResult(boolean isSuccess, String message) {
        javafx.application.Platform.runLater(() -> {
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
        });
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