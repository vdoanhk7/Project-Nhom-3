package com.nhom3.client.controller;

import com.nhom3.shared.model.item.Item;
import com.nhom3.shared.model.auction.Auction; 
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class ViewItemDetailController {

    @FXML private Label lblItemName, lblItemType, lblStatusBadge;
    @FXML private Label lblCurrentPrice, lblStartPrice;
    @FXML private Label lblTimeTitle, lblCountdown, lblTimeRange;

    // Giữ lại khai báo bảng để FXML không bị lỗi, nhưng chưa dùng đến
    @FXML private TableView<?> tableBids; 
    
    private Timeline countdownTimeline;
    private Auction currentAuction;
    //Tự động cập nhật thời gian
    private void refreshState() {
        if (currentAuction == null) return;
        
        LocalDateTime now = LocalDateTime.now();
        
        // Tự động kiểm tra thời gian thực
        if (now.isBefore(currentAuction.getStartTime())) {
            setupOpenState(currentAuction);
        } else if (now.isBefore(currentAuction.getEndTime())) {
            setupRunningState(currentAuction);
        } else {
            setupFinishedState(currentAuction);
        }
    }

    @FXML
    public void initialize() {
        // Tạm thời để trống, sau này làm phần Bid sẽ thêm logic cấu hình cột vào đây
    }

    public void setItemData(Item item, Auction auction, String status) {
        lblItemName.setText(item.getName());
        lblItemType.setText("Phân loại: " + item.getType());
        lblStartPrice.setText(String.format("Khởi điểm: %,.0f VNĐ", item.getStartPrice()));
        lblCurrentPrice.setText(String.format("%,.0f VNĐ", item.getCurHighest()));
        this.currentAuction = auction;
        if (auction == null) {
            lblStatusBadge.setText("CHƯA ĐĂNG BÁN");
            lblStatusBadge.setStyle("-fx-background-color: #95a5a6;");
            lblCountdown.setText("--:--:--");
            lblTimeTitle.setText("THỜI GIAN:");
            lblTimeRange.setText("");
            return;
        }

        if ("PAID".equals(status)) {
            setupPaidState(auction);
        } else if ("CANCELLED".equals(status)) {
            setupCancelledState(auction);
        } else {
            refreshState();}
    }

    private void setupOpenState(Auction auction) {
        lblStatusBadge.setText(" SẮP DIỄN RA ");
        lblStatusBadge.setStyle("-fx-background-color: #f1c40f; -fx-text-fill: black;");
        lblTimeTitle.setText("BẮT ĐẦU SAU:");
        lblTimeRange.setText("Lên sàn lúc: " + formatTime(auction.getStartTime()));
        startCountdown(auction.getStartTime());
    }

    private void setupRunningState(Auction auction) {
        lblStatusBadge.setText(" ĐANG ĐẤU GIÁ ");
        lblStatusBadge.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        lblTimeTitle.setText("THỜI GIAN CÒN LẠI:");
        lblTimeRange.setText("Kết thúc lúc: " + formatTime(auction.getEndTime()));
        startCountdown(auction.getEndTime());
    }

    private void setupFinishedState(Auction auction) {
        lblStatusBadge.setText(" ĐÃ KẾT THÚC ");
        lblStatusBadge.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
        lblTimeTitle.setText("PHIÊN ĐÃ ĐÓNG");
        lblCountdown.setText("00:00:00");
        lblTimeRange.setText("Đã đóng lúc: " + formatTime(auction.getEndTime()));
        if (countdownTimeline != null) countdownTimeline.stop();
    }

    private void setupPaidState(Auction auction) {
        lblStatusBadge.setText("ĐÃ THANH TOÁN");
        lblStatusBadge.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;"); // Màu xanh lá
        lblTimeTitle.setText("GIAO DỊCH HOÀN TẤT");
        lblCountdown.setText("DONE");
        lblTimeRange.setText("Kết thúc lúc: " + formatTime(auction.getEndTime()));
        if (countdownTimeline != null) countdownTimeline.stop();
    }

    private void setupCancelledState(Auction auction) {
        lblStatusBadge.setText("ĐÃ HỦY BỎ");
        lblStatusBadge.setStyle("-fx-background-color: #7f8c8d; -fx-text-fill: white;"); // Màu xám xám
        lblTimeTitle.setText("PHIÊN ĐÃ ĐÓNG");
        lblCountdown.setText("--:--:--");
        lblTimeRange.setText("Lý do: Theo quy định hệ thống");
        if (countdownTimeline != null) countdownTimeline.stop();
    }

    private void startCountdown(LocalDateTime targetTime) {
        if (countdownTimeline != null) countdownTimeline.stop();

        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            long secondsDiff = ChronoUnit.SECONDS.between(LocalDateTime.now(), targetTime);
            if (secondsDiff <= 0) {
                countdownTimeline.stop();
                refreshState(); // Khi đếm ngược kết thúc, tự động cập nhật trạng thái mới
            } else {
                long hours = secondsDiff / 3600;
                long minutes = (secondsDiff % 3600) / 60;
                long seconds = secondsDiff % 60;
                lblCountdown.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
            }
        }));
        countdownTimeline.setCycleCount(Animation.INDEFINITE);
        countdownTimeline.play();
    }

    @FXML
    private void handleClose() {
        if (countdownTimeline != null) countdownTimeline.stop();
        ((Stage) lblItemName.getScene().getWindow()).close();
    }

    private String formatTime(LocalDateTime time) {
        return time.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm - dd/MM"));
    }
}