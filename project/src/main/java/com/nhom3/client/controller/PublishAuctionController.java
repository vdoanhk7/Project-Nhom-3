package com.nhom3.client.controller;

import com.nhom3.shared.model.auction.Auction;
import com.nhom3.shared.model.item.Item;
import com.nhom3.server.dao.AuctionDAO;
import com.nhom3.server.dao.AuctionDAOImpl;
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

    public void setItem(Item item) {
        this.currentItem = item;
        lblItemName.setText(item.getName());
        dpStartDate.setValue(LocalDate.now());
        dpEndDate.setValue(LocalDate.now().plusDays(1)); // Mặc định kết thúc sau 1 ngày
    }

    @FXML
    void handleConfirm() {
        try {
            // 1. Chuyển đổi dữ liệu nhập liệu thành LocalDateTime
            LocalDateTime start = LocalDateTime.of(dpStartDate.getValue(), LocalTime.parse(txtStartTime.getText()));
            LocalDateTime end = LocalDateTime.of(dpEndDate.getValue(), LocalTime.parse(txtEndTime.getText()));
            // 2. Kiểm tra logic thời gian
            if (start.isBefore(LocalDateTime.now())) {
                showAlert("Lỗi", "Thời gian bắt đầu không được ở trong quá khứ!");
                return;
            }
            if (end.isBefore(start)) {
                showAlert("Lỗi", "Thời gian kết thúc phải sau thời gian bắt đầu!");
                return;
            }
            // 3. Tạo Auction và lưu vào DB
            Auction newAuction = new Auction(0, currentItem, start, end);
            AuctionDAO auctionDAO = new AuctionDAOImpl();    
            if (auctionDAO.createAuction(newAuction)) {
                showAlert("Thành công", "Sản phẩm đã được lên lịch đấu giá thành công!");
                ((Stage) lblItemName.getScene().getWindow()).close();
            } else {
                showAlert("Lỗi", "Không thể tạo phiên đấu giá. Vui lòng thử lại!");
            }
        } catch (Exception e) {
            showAlert("Lỗi định dạng", "Vui lòng nhập giờ đúng định dạng HH:mm (VD: 08:30)");
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