package com.nhom3.client.controller;

import com.nhom3.client.event.ClientEventBus;
import com.nhom3.client.event.ClientEvents;
import com.nhom3.client.event.ControllerLifecycle;
import com.nhom3.client.utils.DialogUtils;
import com.nhom3.client.utils.UserSession;
import com.nhom3.shared.model.item.Item;
import com.nhom3.shared.model.user.User;
import com.nhom3.client.utils.MoneyInputFormatter;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

public class PublishAuctionController {
    @FXML private Label lblItemName;
    @FXML private RadioButton rbPublishNow;
    @FXML private RadioButton rbSchedule;
    @FXML private DatePicker dpStartDate, dpEndDate;
    @FXML private TextField txtStartTime, txtEndTime;
    @FXML private TextField txtBidStep;
    @FXML private javafx.scene.image.ImageView imgPreview;

    // Khai báo thêm 2 nút để cấu hình hiệu ứng Hover ở initialize
    @FXML private Button btnCancel;
    @FXML private Button btnConfirm;

    private Item currentItem;
    private boolean waitingForPublishResult;
    private final ToggleGroup publishModeGroup = new ToggleGroup();
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm").withResolverStyle(ResolverStyle.STRICT);
    private static final int MAX_AUCTION_YEAR_OFFSET = 10;

    @FXML
    public void initialize() {
        ClientEventBus.getDefault().subscribe(
                ClientEvents.PublishAuctionResult.class, this, PublishAuctionController::handlePublishEvent);
        ControllerLifecycle.unsubscribeOnDetach(lblItemName, this);
        rbPublishNow.setToggleGroup(publishModeGroup);
        rbSchedule.setToggleGroup(publishModeGroup);
        rbSchedule.setSelected(true);
        publishModeGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> updatePublishModeUI());
        MoneyInputFormatter.install(txtBidStep);
        
        setupButtonStyles(); // Gọi hàm làm đẹp nút bấm
        updatePublishModeUI();
    }

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
        txtBidStep.setText(MoneyInputFormatter.formatAmount(com.nhom3.shared.model.auction.Auction.DEFAULT_BID_STEP));
        
        if (item.getImageBase64() != null && !item.getImageBase64().isEmpty()) {
            try {
                byte[] imageBytes = java.util.Base64.getDecoder().decode(item.getImageBase64());
                imgPreview.setImage(new javafx.scene.image.Image(new java.io.ByteArrayInputStream(imageBytes)));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
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
        if (waitingForPublishResult) {
            return;
        }

        try {
            LocalDateTime start;
            if (isPublishNowMode()) {
                start = LocalDateTime.now();
            } else {
                LocalDate startDate = readAuctionDate(dpStartDate, "Ngày bắt đầu");
                LocalTime startTime = readAuctionTime(txtStartTime, "Giờ bắt đầu");
                start = LocalDateTime.of(startDate, startTime);
                if (start.isBefore(LocalDateTime.now())) {
                    showAlert("Lỗi", "Thời gian bắt đầu không được ở trong quá khứ!");
                    return;
                }
            }
            LocalDate endDate = readAuctionDate(dpEndDate, "Ngày kết thúc");
            LocalTime endTime = readAuctionTime(txtEndTime, "Giờ kết thúc");
            LocalDateTime end = LocalDateTime.of(endDate, endTime);

            if (end.isBefore(LocalDateTime.now())) {
                showAlert("Lỗi", "Thời gian kết thúc không được ở trong quá khứ!");
                return;
            }
            
            if (!end.isAfter(start)) {
                showAlert("Lỗi", "Thời gian kết thúc phải sau thời gian bắt đầu!");
                return;
            }

            User currentUser = UserSession.getInstance().getLoggedInUser();
            if (currentUser == null) {
                showAlert("Lỗi", "Không tìm thấy phiên đăng nhập!");
                return;
            }

            double bidStep = MoneyInputFormatter.parseAmount(txtBidStep.getText());
            if (bidStep <= 0) {
                showAlert("Lỗi", "Bước giá phải lớn hơn 0!");
                return;
            }

            // --- BẮT ĐẦU GỬI MẠNG ---
            com.nhom3.shared.network.payload.PublishAuctionPayload payload = new com.nhom3.shared.network.payload.PublishAuctionPayload(
                currentItem.getId(), start.toString(), end.toString(), bidStep, currentUser.getId()
            );
            com.nhom3.shared.network.packet.Packet packet = new com.nhom3.shared.network.packet.Packet(com.nhom3.shared.network.packet.PacketType.PUBLISH_AUCTION, payload);
            
            setPublishPending(true);
            com.nhom3.client.network.ServerConnection.getInstance().sendMessage(packet);
            System.out.println("[Client] Đã gửi yêu cầu đăng bán sản phẩm ID: " + currentItem.getId());

        } catch (InvalidAuctionDateException e) {
            setPublishPending(false);
            showAlert("Lỗi ngày tháng", e.getMessage());
        } catch (InvalidAuctionTimeException e) {
            setPublishPending(false);
            showAlert("Lỗi giờ", e.getMessage());
        } catch (Exception e) {
            setPublishPending(false);
            e.printStackTrace();
            showAlert("Lỗi định dạng", "Vui lòng kiểm tra lại bước giá.");
        }
    }

    public void handlePublishResult(boolean isSuccess, String message) {
        if (!waitingForPublishResult) {
            return;
        }
        setPublishPending(false);

        Platform.runLater(() -> {
            if (isSuccess) {
                String successMessage = isPublishNowMode()
                        ? "Sản phẩm đã được đăng bán ngay thành công!"
                        : "Sản phẩm đã được lên lịch đấu giá thành công!";
                DialogUtils.showAlertAsync(Alert.AlertType.INFORMATION, "Thành công", successMessage, lblItemName, () -> {
                    closeWindow();
                    ClientEventBus.getDefault().publish(new ClientEvents.SellerItemsChanged());
                });
            } else {
                showAlert("Lỗi", message);
            }
        });
    }

    private void handlePublishEvent(ClientEvents.PublishAuctionResult event) {
        handlePublishResult(event.success(), event.message());
    }

    private void setPublishPending(boolean pending) {
        waitingForPublishResult = pending;
        Runnable updateButtons = () -> {
            if (btnConfirm != null) {
                btnConfirm.setDisable(pending);
            }
            if (btnCancel != null) {
                btnCancel.setDisable(pending);
            }
        };
        if (Platform.isFxApplicationThread()) {
            updateButtons.run();
        } else {
            Platform.runLater(updateButtons);
        }
    }

    @FXML void handleCancel() { closeWindow(); }

    private void closeWindow() {
        ((Stage) lblItemName.getScene().getWindow()).close();
    }

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

    private LocalDate readAuctionDate(DatePicker datePicker, String fieldName) {
        String editorText = datePicker.getEditor().getText();
        LocalDate date;
        try {
            date = editorText == null || editorText.isBlank()
                    ? datePicker.getValue()
                    : datePicker.getConverter().fromString(editorText.trim());
        } catch (RuntimeException e) {
            throw new InvalidAuctionDateException(
                    fieldName + " không hợp lệ. Vui lòng nhập ngày đúng định dạng của ô chọn ngày.");
        }

        if (date == null) {
            throw new InvalidAuctionDateException(fieldName + " không hợp lệ.");
        }

        validateAuctionYear(date, fieldName);
        datePicker.setValue(date);
        return date;
    }

    private LocalTime readAuctionTime(TextField timeField, String fieldName) {
        String timeText = timeField.getText();
        if (timeText == null || timeText.isBlank()) {
            throw new InvalidAuctionTimeException(fieldName + " không được để trống.");
        }

        try {
            LocalTime time = LocalTime.parse(timeText.trim(), TIME_FORMATTER);
            timeField.setText(time.format(TIME_FORMATTER));
            return time;
        } catch (RuntimeException e) {
            throw new InvalidAuctionTimeException(
                    fieldName + " không hợp lệ. Vui lòng nhập đúng định dạng HH:mm, ví dụ 08:00.");
        }
    }

    private void validateAuctionYear(LocalDate date, String fieldName) {
        int currentYear = LocalDate.now().getYear();
        int maxYear = currentYear + MAX_AUCTION_YEAR_OFFSET;
        int year = date.getYear();
        if (year < currentYear || year > maxYear) {
            throw new InvalidAuctionDateException(
                    fieldName + " quá xa so với hiện tại.");
        }
    }

    private static class InvalidAuctionDateException extends RuntimeException {
        InvalidAuctionDateException(String message) {
            super(message);
        }
    }

    private static class InvalidAuctionTimeException extends RuntimeException {
        InvalidAuctionTimeException(String message) {
            super(message);
        }
    }

    private void showAlert(String title, String content) {
        DialogUtils.showAlertAsync(Alert.AlertType.INFORMATION, title, content, lblItemName);
    }
}
