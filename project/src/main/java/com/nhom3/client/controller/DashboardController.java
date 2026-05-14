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
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
        value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD",
        justification = "JavaFX controllers are reached from the socket dispatcher through the active screen instance.")
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

    // Singleton để ServerHandler gọi về
    private static DashboardController instance;
    public static DashboardController getInstance() { 
        return instance; 
    }
    @FXML
    public void initialize() {
        instance = this;
        setupTables();
        
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
        showAlert(Alert.AlertType.INFORMATION, "Hướng dẫn", "Vui lòng chọn nút 'Chợ Đấu Giá' ở thanh Menu bên trái để xem các sản phẩm đang lên sàn.");
    }

    @FXML
    void handleGoToAssetManagement(ActionEvent event) {
        User currentUser = UserSession.getInstance().getLoggedInUser();
        if (currentUser != null && currentUser.getRole() == Role.SELLER) {
            showAlert(Alert.AlertType.INFORMATION, "Hướng dẫn", "Vui lòng chọn nút 'Quản Lý Sản Phẩm' ở thanh Menu bên trái.");
        } else {
            showAlert(Alert.AlertType.WARNING, "Từ chối truy cập", "Chỉ tài khoản Người Bán (Seller) mới có thể sử dụng tính năng này!");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type); alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(content); alert.showAndWait();
    }
}
