package com.nhom3.client.controller;

import com.nhom3.client.event.ClientEventBus;
import com.nhom3.client.event.ClientEvents;
import com.nhom3.client.event.ControllerLifecycle;
import com.nhom3.client.utils.ChatbotLogic;
import com.nhom3.client.utils.ChatbotLogic.ChatResult;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
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
import com.nhom3.client.utils.DialogUtils;
import com.nhom3.client.utils.UserSession;
import com.nhom3.shared.model.user.User;
import com.nhom3.shared.model.user.Admin;
import com.nhom3.shared.model.user.Seller;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class MainController {
    private static final double LOADING_SPINNER_SIZE = 50;
    private static final double DEFAULT_STAGE_MIN_SIZE = 0;
    private static final int MAX_CHAT_MESSAGES = 50;

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
    @FXML private VBox chatPanel;
    @FXML private ScrollPane chatScrollPane;
    @FXML private VBox chatMessageContainer;
    @FXML private TextField txtChatInput;
    @FXML private Button btnSendChat;

    private long currentNavigationId = 0; 
    private boolean accountDeletedHandled;
    private final ChatbotLogic chatbotLogic = new ChatbotLogic();
    private String currentChatContext = "DEFAULT";
    private Object currentContextData = null;
    
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
        ClientEventBus.getDefault().subscribe(
                ClientEvents.ChatContextChanged.class, this, MainController::handleChatContextChanged);
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

    @FXML
    void toggleChat(ActionEvent event) {
        boolean shouldShow = !chatPanel.isVisible();
        chatPanel.setVisible(shouldShow);
        chatPanel.setManaged(shouldShow);

        if (shouldShow) {
            chatMessageContainer.getChildren().clear();
            appendOpeningChatBubble();
            txtChatInput.requestFocus();
        }
    }

    private void appendOpeningChatBubble() {
        appendChatBubble(chatbotLogic.getContextualGreeting(currentChatContext, currentContextData), false);
    }

    @FXML
    void handleSendChat(ActionEvent event) {
        String input = txtChatInput.getText();
        sendChatMessage(input);
    }

    private void sendChatMessage(String input) {
        if (input == null || input.trim().isEmpty()) {
            return;
        }

        input = input.trim();
        txtChatInput.clear();
        appendChatBubble(input, true);
        setChatInputEnabled(false);

        final String userInput = input;
        Thread responseThread = new Thread(() -> {
            try {
                Thread.sleep(ThreadLocalRandom.current().nextInt(300, 501));
                ChatResult result = chatbotLogic.getResponse(userInput);
                Platform.runLater(() -> {
                    appendChatBubble(result, false);
                    handleChatAction(result.getActionCode());
                    setChatInputEnabled(true);
                    txtChatInput.requestFocus();
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Platform.runLater(() -> setChatInputEnabled(true));
            }
        });
        responseThread.setDaemon(true);
        responseThread.start();
    }

    private void handleChatAction(String actionCode) {
        if (actionCode == null || actionCode.isBlank()) {
            return;
        }

        switch (actionCode) {
            case "NAV_MARKET" -> ClientEventBus.getDefault().publish(
                    new ClientEvents.NavigationRequested(ClientEvents.Route.MARKET));
            case "NAV_DASHBOARD" -> ClientEventBus.getDefault().publish(
                    new ClientEvents.NavigationRequested(ClientEvents.Route.DASHBOARD));
            case "NAV_PROFILE" -> ClientEventBus.getDefault().publish(
                    new ClientEvents.NavigationRequested(ClientEvents.Route.PROFILE));
            case "NAV_PURCHASE_HISTORY" -> ClientEventBus.getDefault().publish(
                    new ClientEvents.NavigationRequested(ClientEvents.Route.PURCHASE_HISTORY));
            case "NAV_MANAGE_ITEM" -> {
                if (UserSession.getInstance().getLoggedInUser() instanceof Seller) {
                    ClientEventBus.getDefault().publish(
                            new ClientEvents.NavigationRequested(ClientEvents.Route.MANAGE_ITEM));
                } else {
                    appendChatBubble("Xin lỗi, chỉ có Người bán (Seller) mới dùng được chức năng này.", false);
                }
            }
            default -> {
            }
        }
    }

    private void appendChatBubble(String text, boolean isUser) {
        appendChatBubble(new ChatResult(text, null), isUser);
    }

    private void appendChatBubble(ChatResult result, boolean isUser) {
        HBox row = new HBox();
        row.setAlignment(isUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        Label bubble = new Label(result.getMessage());
        bubble.setWrapText(true);
        bubble.setMinHeight(Region.USE_PREF_SIZE);
        bubble.setMaxWidth(isUser ? 250 : 300);
        bubble.setStyle(isUser
                ? "-fx-background-color: #3498db; -fx-text-fill: white; -fx-padding: 10 12 10 12; -fx-background-radius: 16 16 4 16;"
                : "-fx-background-color: #e9edf3; -fx-text-fill: #2c3e50; -fx-padding: 10 12 10 12; -fx-background-radius: 16 16 16 4;");

        row.setMinHeight(Region.USE_PREF_SIZE);
        row.getChildren().add(bubble);

        if (!isUser && result.getSuggestions() != null && !result.getSuggestions().isEmpty()) {
            VBox botBlock = new VBox(6);
            botBlock.setAlignment(Pos.CENTER_LEFT);
            botBlock.setMinHeight(Region.USE_PREF_SIZE);
            botBlock.getChildren().add(row);

            FlowPane suggestionPane = new FlowPane(5, 5);
            suggestionPane.setMaxWidth(300);
            suggestionPane.setMinHeight(Region.USE_PREF_SIZE);

            List<Button> suggestionButtons = new java.util.ArrayList<>();
            for (String suggestion : result.getSuggestions()) {
                Button suggestionButton = createSuggestionButton(suggestion);
                suggestionButtons.add(suggestionButton);
                suggestionButton.setOnAction(event -> {
                    suggestionButtons.forEach(button -> button.setDisable(true));
                    sendChatMessage(suggestionButton.getText());
                });
                suggestionPane.getChildren().add(suggestionButton);
            }

            botBlock.getChildren().add(suggestionPane);
            chatMessageContainer.getChildren().add(botBlock);
        } else {
            chatMessageContainer.getChildren().add(row);
        }

        while (chatMessageContainer.getChildren().size() > MAX_CHAT_MESSAGES) {
            chatMessageContainer.getChildren().remove(0);
        }

        Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
    }

    private Button createSuggestionButton(String text) {
        Button button = new Button(text);
        String normalStyle = "-fx-background-color: #ffffff; -fx-text-fill: #3498db; "
                + "-fx-border-color: #b8dcf4; -fx-border-radius: 18; -fx-background-radius: 18; "
                + "-fx-padding: 6 12 6 12; -fx-cursor: hand; -fx-font-size: 12px;";
        String hoverStyle = "-fx-background-color: #eaf6fd; -fx-text-fill: #2980b9; "
                + "-fx-border-color: #7fc3ec; -fx-border-radius: 18; -fx-background-radius: 18; "
                + "-fx-padding: 6 12 6 12; -fx-cursor: hand; -fx-font-size: 12px;";
        button.setStyle(normalStyle);
        button.setOnMouseEntered(event -> {
            if (!button.isDisabled()) {
                button.setStyle(hoverStyle);
            }
        });
        button.setOnMouseExited(event -> button.setStyle(normalStyle));
        return button;
    }

    private void setChatInputEnabled(boolean enabled) {
        txtChatInput.setDisable(!enabled);
        btnSendChat.setDisable(!enabled);
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

    private void handleChatContextChanged(ClientEvents.ChatContextChanged event) {
        currentChatContext = event.contextType() == null || event.contextType().isBlank()
                ? "DEFAULT"
                : event.contextType();
        currentContextData = event.data();
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
        DialogUtils.initOwner(alert, contentArea);
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
        setChatContext("DASHBOARD", null);
        setActiveButton(btnDashboard); 
        loadPage("/com/nhom3/client/view/dashboard.fxml");
    }

    public void navigateToMarket() {
        setChatContext("MARKET", null);
        setActiveButton(btnMarket); 
        loadPage("/com/nhom3/client/view/market.fxml");
    }

    public void navigateToPurchaseHistory() {
        setChatContext("PURCHASE_HISTORY", null);
        setActiveButton(btnPurchaseHistory); 
        loadPage("/com/nhom3/client/view/purchase_history.fxml");
    }

    public void navigateToManageItem() {
        setChatContext("MANAGE_ITEM", null);
        setActiveButton(btnManageItem); 
        loadPage("/com/nhom3/client/view/manage_item.fxml");
    }

    public void navigateToAdminPanel() {
        setChatContext("ADMIN_PANEL", null);
        setActiveButton(btnAdminPanel); 
        loadPage("/com/nhom3/client/view/admin_dashboard.fxml");
    }
    
    public void navigateToProfile() {
        setChatContext("PROFILE", null);
        setActiveButton(btnProfile); 
        loadPage("/com/nhom3/client/view/profile.fxml");
    }

    private void setChatContext(String contextType, Object data) {
        currentChatContext = contextType == null || contextType.isBlank() ? "DEFAULT" : contextType;
        currentContextData = data;
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
        DialogUtils.initOwner(alert, (Node) event.getSource());

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
            stage.setMaximized(false);
            stage.setMinWidth(DEFAULT_STAGE_MIN_SIZE);
            stage.setMinHeight(DEFAULT_STAGE_MIN_SIZE);
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.sizeToScene();
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
