package com.nhom3.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;
import javafx.scene.control.Label;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Alert;
import javafx.scene.Node;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainController {

    // Ánh xạ tới vùng Center của BorderPane trong file FXML
    @FXML
    private StackPane contentArea;

    // Hàm chạy ngay khi giao diện vừa load lên
    @FXML
    public void initialize() {
        System.out.println("Giao diện chính đã tải thành công!");
        // Thường người ta sẽ gọi showDashboard() ở đây để load trang chủ mặc định
    }

    // Sự kiện khi bấm nút Tổng quan
    @FXML
    void showDashboard(ActionEvent event) {
        // Trong thực tế, bạn sẽ dùng FXMLLoader để load file view_dashboard.fxml
        // Demo tạm bằng cách thay đổi text:
        contentArea.getChildren().clear();
        contentArea.getChildren().add(new Label("Đang hiển thị: TỔNG QUAN"));
        System.out.println("Chuyển sang màn hình Tổng quan");
    }

    // Sự kiện khi bấm nút Chợ Đấu Giá
    @FXML
    void showMarket(ActionEvent event) {
        // Tương lai: FXMLLoader.load(getClass().getResource("/com/nhom3/view_market.fxml"));
        contentArea.getChildren().clear();
        contentArea.getChildren().add(new Label("Đang hiển thị: CHỢ ĐẤU GIÁ"));
        System.out.println("Chuyển sang màn hình Chợ Đấu Giá");
    }

    // Sự kiện khi bấm nút Vật phẩm của tôi
    @FXML
    void showMyItems(ActionEvent event) {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(new Label("Đang hiển thị: VẬT PHẨM CỦA TÔI"));
    }

    //Sự kiện khi bấm nút Thông tin cá nhân
    @FXML
    void showProfile(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/nhom3/client/view/profile.fxml"));
            Parent view = loader.load();
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
        } catch (Exception e) { e.printStackTrace(); }
    }

    // Sự kiện khi bấm nút Đăng xuất
    @FXML
    void handleLogout(ActionEvent event) {
        // 1. Tạo hộp thoại xác nhận (CONFIRMATION)
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận");
        alert.setHeaderText(null);
        alert.setContentText("Bạn thực sự muốn đăng xuất?");

        // 2. Bắt sự kiện khi người dùng bấm nút trên thông báo
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/nhom3/client/view/login.fxml"));
                Parent root = loader.load();
                
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.centerOnScreen(); 
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}