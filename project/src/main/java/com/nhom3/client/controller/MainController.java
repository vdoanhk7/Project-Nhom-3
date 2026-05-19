package com.nhom3.client.controller;

import com.nhom3.client.event.ClientEventBus;
import com.nhom3.client.event.ClientEvents;
import com.nhom3.client.event.ControllerLifecycle;
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
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import javafx.scene.control.Button; 
import com.nhom3.client.utils.UserSession;
import com.nhom3.shared.model.user.User;
import com.nhom3.shared.model.user.Admin;
import com.nhom3.shared.model.user.Seller;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

public class MainController {
    private static final double LOADING_SPINNER_SIZE = 50;

    @FXML private StackPane contentArea;
    @FXML private Label lblUserName;
    @FXML private Circle circleUserAvatar;
    @FXML private Circle circleMenuProfileAvatar;
    
    @FXML private Button btnDashboard;
    @FXML private Button btnMarket;
    @FXML private Button btnPurchaseHistory;
    @FXML private Button btnManageItem;
    @FXML private Button btnAdminPanel;
    @FXML private Button btnProfile;

    private long currentNavigationId = 0; 
    private boolean accountDeletedHandled;
    
    private List<Button> allMenuButtons; 

    @FXML
    public void initialize() {
        System.out.println("Giao diện chính đã tải thành công!");
        ClientEventBus.getDefault().subscribe(
                ClientEvents.NavigationRequested.class, this, MainController::handleNavigationRequested);
        ClientEventBus.getDefault().subscribe(
                ClientEvents.UserProfileChanged.class, this, MainController::handleUserProfileChanged);
        ClientEventBus.getDefault().subscribe(
                ClientEvents.AccountDeleted.class, this, MainController::handleAccountDeleted);
        ControllerLifecycle.unsubscribeOnDetach(contentArea, this);
        
        allMenuButtons = Arrays.asList(btnDashboard, btnMarket, btnPurchaseHistory, btnManageItem, btnAdminPanel, btnProfile);
        
        btnPurchaseHistory.setVisible(false); btnPurchaseHistory.setManaged(false);
        btnManageItem.setVisible(false); btnManageItem.setManaged(false);
        btnAdminPanel.setVisible(false); btnAdminPanel.setManaged(false);

        User currentUser = UserSession.getInstance().getLoggedInUser();
        
        if (currentUser != null) {
            refreshUserProfileHeader();

            if (currentUser instanceof Admin) {
                btnAdminPanel.setVisible(true); btnAdminPanel.setManaged(true);
            } else if (currentUser instanceof Seller) {
                btnManageItem.setVisible(true); btnManageItem.setManaged(true);
            } else {
                btnPurchaseHistory.setVisible(true); btnPurchaseHistory.setManaged(true);
            }
        }
        System.out.println("Đang tự động tải trang Tổng quan mặc định...");
        navigateToDashboard(); 
    }

    private void handleNavigationRequested(ClientEvents.NavigationRequested event) {
        switch (event.route()) {
            case DASHBOARD -> navigateToDashboard();
            case MARKET -> navigateToMarket();
            case PURCHASE_HISTORY -> navigateToPurchaseHistory();
            case MANAGE_ITEM -> navigateToManageItem();
            case ADMIN_PANEL -> navigateToAdminPanel();
            case PROFILE -> navigateToProfile();
        }
    }

    private void handleUserProfileChanged(ClientEvents.UserProfileChanged event) {
        refreshUserProfileHeader();
    }

    private void handleAccountDeleted(ClientEvents.AccountDeleted event) {
        if (accountDeletedHandled) {
            return;
        }
        accountDeletedHandled = true;
        UserSession.getInstance().logout();

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Tài khoản đã bị xóa");
        alert.setHeaderText(null);
        alert.setContentText(event.message());
        alert.showAndWait();

        showLoginScene();
    }

    public void refreshUserProfileHeader() {
        User currentUser = UserSession.getInstance().getLoggedInUser();
        if (currentUser == null) return;

        lblUserName.setText(buildGreeting(currentUser));
        btnProfile.setText(buildProfileMenuText(currentUser));
        renderHeaderAvatar(currentUser);
    }

    private void renderHeaderAvatar(User user) {
        if (user.getUserInfo() == null) {
            return;
        }

        String profileImage = user.getUserInfo().getProfileImageBase64();
        if (profileImage == null || profileImage.isBlank()) {
            setProfileAvatarFill(Color.web("#95a5a6"));
            return;
        }

        try {
            byte[] imageBytes = Base64.getDecoder().decode(profileImage.trim());
            Image image = new Image(new ByteArrayInputStream(imageBytes));
            if (image.isError()) {
                setProfileAvatarFill(Color.web("#95a5a6"));
                return;
            }
            setProfileAvatarFill(new ImagePattern(image));
        } catch (IllegalArgumentException e) {
            setProfileAvatarFill(Color.web("#95a5a6"));
        }
    }

    private void setProfileAvatarFill(Paint fill) {
        if (circleUserAvatar != null) {
            circleUserAvatar.setFill(fill);
        }
        if (circleMenuProfileAvatar != null) {
            circleMenuProfileAvatar.setFill(fill);
        }
    }

    private void setActiveButton(Button activeButton) {
        String normalStyle = "-fx-background-color: transparent; -fx-text-fill: #d1d8e0; -fx-alignment: BASELINE_LEFT; -fx-cursor: hand; -fx-font-size: 15px; -fx-border-width: 0;";
        for (Button btn : allMenuButtons) {
            if (btn != null) btn.setStyle(normalStyle);
        }
        String activeStyle = "-fx-background-color: rgba(255, 255, 255, 0.08); -fx-text-fill: #ffffff; -fx-alignment: BASELINE_LEFT; -fx-cursor: hand; -fx-font-size: 15px; -fx-font-weight: bold; -fx-border-color: #3498db; -fx-border-width: 0 0 0 4;";
        if (activeButton != null) {
            activeButton.setStyle(activeStyle);
        }
    }

    public void loadPage(String fxmlPath) {
        currentNavigationId = System.currentTimeMillis();
        final long thisLoadId = currentNavigationId;
        
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(LOADING_SPINNER_SIZE, LOADING_SPINNER_SIZE);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(spinner);
        
        Thread loadThread = new Thread(() -> {
            javafx.application.Platform.runLater(() -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                    Parent view = loader.load();               
                    if (thisLoadId == currentNavigationId) {
                        contentArea.getChildren().clear();
                        contentArea.getChildren().add(view);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    if (thisLoadId == currentNavigationId) {
                        contentArea.getChildren().clear();
                        Label lblError = new Label("Lỗi: Không thể tải giao diện!");
                        lblError.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                        contentArea.getChildren().add(lblError);
                    }
                }
            });
        });

        loadThread.setDaemon(true); 
        loadThread.start();
    }
    
    @FXML void showDashboard(ActionEvent event) { navigateToDashboard(); }
    @FXML void showMarket(ActionEvent event) { navigateToMarket(); }
    @FXML void showPurchaseHistory(ActionEvent event) { navigateToPurchaseHistory(); }
    @FXML void showManageItem(ActionEvent event) { navigateToManageItem(); }
    @FXML void showAdminPanel(ActionEvent event) { navigateToAdminPanel(); }
    @FXML void showProfile(ActionEvent event) { navigateToProfile(); }

    public void navigateToDashboard() {
        setActiveButton(btnDashboard); 
        loadPage("/com/nhom3/client/view/dashboard.fxml");
    }

    public void navigateToMarket() {
        setActiveButton(btnMarket); 
        loadPage("/com/nhom3/client/view/market.fxml");
    }

    public void navigateToPurchaseHistory() {
        setActiveButton(btnPurchaseHistory); 
        loadPage("/com/nhom3/client/view/purchase_history.fxml");
    }

    public void navigateToManageItem() {
        setActiveButton(btnManageItem); 
        loadPage("/com/nhom3/client/view/manage_item.fxml");
    }

    public void navigateToAdminPanel() {
        setActiveButton(btnAdminPanel); 
        loadPage("/com/nhom3/client/view/admin_dashboard.fxml");
    }
    
    public void navigateToProfile() {
        setActiveButton(btnProfile); 
        loadPage("/com/nhom3/client/view/profile.fxml");
    }

    // HÀM CHÀO HỎI NHƯ BẠN YÊU CẦU
    private String buildGreeting(User user) {
        return "Xin chào " + user.getRole().name() + ", " + user.getUserInfo().getName();
    }

    private String buildProfileMenuText(User user) {
        String name = user.getUserInfo() != null ? user.getUserInfo().getName() : "";
        return name != null && !name.isBlank() ? name : "Hồ sơ cá nhân";
    }

    @FXML
    void handleLogout(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận");
        alert.setHeaderText(null);
        alert.setContentText("Bạn thực sự muốn đăng xuất?");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            UserSession.getInstance().logout();
            showLoginScene((Stage) ((Node) event.getSource()).getScene().getWindow());
        }
    }

    private void showLoginScene() {
        if (contentArea == null || contentArea.getScene() == null) {
            return;
        }
        showLoginScene((Stage) contentArea.getScene().getWindow());
    }

    private void showLoginScene(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/nhom3/client/view/login.fxml"));
            Parent root = loader.load();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
