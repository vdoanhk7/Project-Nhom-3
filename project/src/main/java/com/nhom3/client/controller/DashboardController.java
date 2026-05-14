package com.nhom3.client.controller;

import com.nhom3.client.network.ServerConnection;
import com.nhom3.client.utils.UserSession;
import com.nhom3.shared.model.user.Role;
import com.nhom3.shared.model.user.User;
import com.nhom3.shared.network.packet.Packet;
import com.nhom3.shared.network.packet.PacketType;
import com.nhom3.shared.network.payload.DashboardResponsePayload;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class DashboardController {

    @FXML private Label lblTotalUsers;
    @FXML private Label lblActiveItems;
    @FXML private Label lblTotalRevenue;

    @FXML private TableView<DashboardResponsePayload.TopBidderDTO> tableTopBidders;
    @FXML private TableColumn<DashboardResponsePayload.TopBidderDTO, Integer> colBidderRank;
    @FXML private TableColumn<DashboardResponsePayload.TopBidderDTO, String> colBidderName;
    @FXML private TableColumn<DashboardResponsePayload.TopBidderDTO, String> colBidderSpent;

    @FXML private TableView<DashboardResponsePayload.TopItemDTO> tableTopItems;
    @FXML private TableColumn<DashboardResponsePayload.TopItemDTO, Integer> colItemRank;
    @FXML private TableColumn<DashboardResponsePayload.TopItemDTO, String> colItemName;
    @FXML private TableColumn<DashboardResponsePayload.TopItemDTO, String> colItemPrice;
    @FXML private Button btnGoToAssetManagement;

    // Singleton để ServerHandler gọi về
    private static DashboardController instance;
    public static DashboardController getInstance() { 
        return instance; 
    }
    @FXML
    public void initialize() {
        instance = this;
        setupTables();
        setupQuickActions();
        
        // Vừa vào màn hình là Gửi yêu cầu qua mạng liền
        try {
            Packet packet = new Packet(PacketType.LOAD_DASHBOARD, null); // Không cần gửi payload đi, chỉ cần gửi Type
            ServerConnection.getInstance().sendMessage(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupTables() {
        colBidderRank.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().rank).asObject());
        colBidderName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().name));
        colBidderSpent.setCellValueFactory(data -> new SimpleStringProperty(String.format("%,.0f", data.getValue().spent)));

        colItemRank.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().rank).asObject());
        colItemName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().name));
        colItemPrice.setCellValueFactory(data -> new SimpleStringProperty(String.format("%,.0f", data.getValue().price)));
    }

    private void setupQuickActions() {
        User currentUser = UserSession.getInstance().getLoggedInUser();
        if (currentUser == null || btnGoToAssetManagement == null) {
            return;
        }

        if (currentUser.getRole() == Role.SELLER) {
            btnGoToAssetManagement.setText("💼 Quản Lý Sản Phẩm");
        } else if (currentUser.getRole() == Role.BIDDER) {
            btnGoToAssetManagement.setText("📦 Lịch Sử Đấu Giá");
        } else {
            btnGoToAssetManagement.setText("⚙️ Quản Trị Hệ Thống");
        }
    }

    // ServerHandler sẽ gọi hàm này khi Database trả kết quả
    public void handleDashboardData(DashboardResponsePayload data) {
        if (data == null) return;

        // 1. Cập nhật thẻ thống kê
        lblTotalUsers.setText(String.format("%,d", data.getTotalUsers()));
        lblActiveItems.setText(String.format("%,d", data.getActiveItems()));
        lblTotalRevenue.setText(String.format("%,.0f", data.getTotalRevenue()));

        // 2. Cập nhật 2 bảng
        tableTopBidders.setItems(FXCollections.observableArrayList(data.getTopBidders()));
        tableTopItems.setItems(FXCollections.observableArrayList(data.getTopItems()));
    }

    @FXML
    void handleGoToMarket(ActionEvent event) {
        MainController mainController = MainController.getInstance();
        if (mainController != null) {
            mainController.navigateToMarket();
            return;
        }
        showAlert(Alert.AlertType.ERROR, "Lỗi điều hướng", "Không thể chuyển sang trang Chợ đấu giá lúc này.");
    }

    @FXML
    void handleGoToAssetManagement(ActionEvent event) {
        User currentUser = UserSession.getInstance().getLoggedInUser();
        MainController mainController = MainController.getInstance();
        if (currentUser == null || mainController == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi điều hướng", "Không thể chuyển trang lúc này.");
            return;
        }

        if (currentUser.getRole() == Role.SELLER) {
            mainController.navigateToManageItem();
        } else if (currentUser.getRole() == Role.BIDDER) {
            mainController.navigateToPurchaseHistory();
        } else {
            mainController.navigateToAdminPanel();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type); alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(content); alert.showAndWait();
    }
}
