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

import javafx.scene.control.Button; 
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

    private long currentNavigationId = 0; // Biến để theo dõi ID của lần điều hướng hiện tại

    @FXML
    public void initialize() {
        System.out.println("Giao diện chính đã tải thành công!");
        
        // 2. ẨN TẤT CẢ CÁC NÚT ĐỘNG
        btnPurchaseHistory.setVisible(false); btnPurchaseHistory.setManaged(false);
        btnManageItem.setVisible(false); btnManageItem.setManaged(false);
        btnAdminPanel.setVisible(false); btnAdminPanel.setManaged(false);

        // 3. LẤY THÔNG TIN USER VÀ PHÂN QUYỀN HIỂN THỊ
        User currentUser = UserSession.getInstance().getLoggedInUser();
        
        if (currentUser != null) {
            lblUserName.setText("Xin chào, " + currentUser.getUserInfo().getName());

            if (currentUser instanceof Admin) {
                btnAdminPanel.setVisible(true); btnAdminPanel.setManaged(true);
            } else if (currentUser instanceof Seller) {
                btnManageItem.setVisible(true); btnManageItem.setManaged(true);
            } else {
                btnPurchaseHistory.setVisible(true); btnPurchaseHistory.setManaged(true);
            }
        }
        System.out.println("Đang tự động tải trang Tổng quan mặc định...");
        loadPage("/com/nhom3/client/view/dashboard.fxml");
    }

    // HÀM TIỆN ÍCH DÙNG CHUNG: Vòng xoay loading & Chống nghẽn luồng
    public void loadPage(String fxmlPath) {
        // 1. Tạo ID mới mỗi lần bấm nút
        currentNavigationId = System.currentTimeMillis();
        final long thisLoadId = currentNavigationId;
        // 2. Hiện vòng xoay
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(50, 50);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(spinner);
        // 3. Tải ngầm FXML
        Thread loadThread = new Thread(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                Parent view = loader.load();               
                // 4. Cập nhật UI nếu ID vẫn khớp (Nghĩa là user chưa bấm nút khác)
                javafx.application.Platform.runLater(() -> {
                    if (thisLoadId == currentNavigationId) {
                        contentArea.getChildren().clear();
                        contentArea.getChildren().add(view);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    if (thisLoadId == currentNavigationId) {
                        contentArea.getChildren().clear();
                        Label lblError = new Label("Lỗi: Không thể tải giao diện!");
                        lblError.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                        contentArea.getChildren().add(lblError);
                    }
                });
            }
        });

        loadThread.setDaemon(true); 
        loadThread.start();
    }
    
    // CÁC SỰ KIỆN CHUYỂN TRANG (ĐÃ ĐƯỢC RÚT GỌN SIÊU SẠCH)
    @FXML
    void showDashboard(ActionEvent event) {
        // Tận dụng ngay hàm tiện ích loadPage siêu sạch đã được định nghĩa
        loadPage("/com/nhom3/client/view/dashboard.fxml");
    }

    @FXML
    void showMarket(ActionEvent event) {
        loadPage("/com/nhom3/client/view/market.fxml");
    }

    @FXML
    void showPurchaseHistory(ActionEvent event) {
        loadPage("/com/nhom3/client/view/purchase_history.fxml");
    }

    @FXML
    void showManageItem(ActionEvent event) {
        loadPage("/com/nhom3/client/view/manage_item.fxml");
    }

    @FXML
    void showAdminPanel(ActionEvent event) {
        loadPage("/com/nhom3/client/view/admin_dashboard.fxml");
    }

    @FXML
    void showProfile(ActionEvent event) {
        loadPage("/com/nhom3/client/view/profile.fxml");
    }

    // ĐĂNG XUẤT
    @FXML
    void handleLogout(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận");
        alert.setHeaderText(null);
        alert.setContentText("Bạn thực sự muốn đăng xuất?");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            // Xóa session người dùng hiện tại (nếu bạn có hàm này trong UserSession)
            // UserSession.getInstance().clearSession(); 

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