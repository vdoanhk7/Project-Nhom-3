package com.nhom3.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Alert;
import javafx.scene.Node;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javafx.scene.control.Button; // Thêm import Button
import com.nhom3.client.utils.UserSession;
import com.nhom3.shared.model.user.User;
import com.nhom3.shared.model.user.Admin;
import com.nhom3.shared.model.user.Seller;
public class MainController {

    @FXML private StackPane contentArea;
    @FXML private Label lblUserName;
    @FXML private Button btnPurchaseHistory;
    @FXML private Button btnManageItem;
    @FXML private Button btnAdminPanel;

    @FXML
    public void initialize() {
        System.out.println("Giao diện chính đã tải thành công!");
        
        // 2. ẨN TẤT CẢ CÁC NÚT ĐỘNG (setManaged = false để nó co lại, không để lại khoảng trống)
        btnPurchaseHistory.setVisible(false); btnPurchaseHistory.setManaged(false);
        btnManageItem.setVisible(false); btnManageItem.setManaged(false);
        btnAdminPanel.setVisible(false); btnAdminPanel.setManaged(false);

        // 3. LẤY THÔNG TIN USER VÀ PHÂN QUYỀN HIỂN THỊ
        User currentUser = UserSession.getInstance().getLoggedInUser();
        
        if (currentUser != null) {
            lblUserName.setText("Xin chào, " + currentUser.getUserInfo().getName());

            // Bật nút theo đúng vai trò
            if (currentUser instanceof Admin) {
                btnAdminPanel.setVisible(true); btnAdminPanel.setManaged(true);
            } else if (currentUser instanceof Seller) {
                btnManageItem.setVisible(true); btnManageItem.setManaged(true);
            } else {
                // Mặc định là Bidder
                btnPurchaseHistory.setVisible(true); btnPurchaseHistory.setManaged(true);
            }
        }
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

    // Sự kiện khi bấm nút Lịch sử đấu giá
    @FXML
    void showPurchaseHistory(ActionEvent event) {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(new Label("Đang hiển thị: LỊCH SỬ ĐẤU GIÁ (BIDDER)"));
    }

    // Sự kiện khi bấm nút Quản lí sản phẩm
    @FXML
    void showManageItem(ActionEvent event) {
        // 1. Tạo vòng xoay Loading
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(50, 50);
        spinner.setLayoutX(contentArea.getWidth() / 2 - 25);
        spinner.setLayoutY(contentArea.getHeight() / 2 - 25);
        // Hiển thị vòng xoay ngay lên màn hình
        contentArea.getChildren().clear();
        contentArea.getChildren().add(spinner);
        // 2. Mở một luồng ngầm (Background Thread) 
        Thread loadThread = new Thread(() -> {
            try {
                // Đọc file FXML ở luồng ngầm 
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/nhom3/client/view/manage_item.fxml"));
                Parent view = loader.load();
                // 3. Khi tải xong, phải dùng Platform.runLater để đẩy kết quả lên UI Thread
                javafx.application.Platform.runLater(() -> {
                    contentArea.getChildren().clear();
                    contentArea.getChildren().add(view);
                });
            } catch (Exception e) {
                e.printStackTrace();
                // Nếu lỗi, báo cho người dùng biết 
                javafx.application.Platform.runLater(() -> {
                    contentArea.getChildren().clear();
                    System.err.println("Lỗi tải giao diện Manage Item!");
                });
            }
        });

        // Set Daemon để Thread tự chết nếu người dùng tắt app giữa chừng
        loadThread.setDaemon(true); 
        loadThread.start();
    }

    // Sự kiện khi bấm nút Quản trị hệ thống
    @FXML
    void showAdminPanel(ActionEvent event) {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(new Label("Đang hiển thị: QUẢN TRỊ HỆ THỐNG (ADMIN)"));
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