package com.nhom3.client.controller;

import com.nhom3.client.event.ClientEventBus;
import com.nhom3.client.event.ClientEvents;
import com.nhom3.client.event.ControllerLifecycle;
import com.nhom3.client.network.ServerConnection;
import com.nhom3.client.utils.DialogUtils;
import com.nhom3.shared.network.packet.Packet;
import com.nhom3.shared.network.packet.PacketType;
import com.nhom3.shared.network.payload.AuctionIdPayload;
import com.nhom3.shared.network.payload.AuctionListResponsePayload;
import com.nhom3.shared.network.payload.UserListResponsePayload;
import java.util.List;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.text.Text;

public class AdminDashboardController {
    private static final double USER_ACTION_BUTTON_SPACING = 5;

    @FXML private Text txtTotalUsers;
    @FXML private Text txtActiveAuctions;
    @FXML private Text txtTotalValue;

    @FXML private TableView<UserListResponsePayload.UserDTO> tableUsers;
    @FXML private TableColumn<UserListResponsePayload.UserDTO, Integer> colUserId;
    @FXML private TableColumn<UserListResponsePayload.UserDTO, String> colUserName;
    @FXML private TableColumn<UserListResponsePayload.UserDTO, String> colUserRole;
    @FXML private TableColumn<UserListResponsePayload.UserDTO, UserListResponsePayload.UserDTO> colUserAction;

    @FXML private TableView<AuctionListResponsePayload.AuctionDTO> tableAuctions;
    @FXML private TableColumn<AuctionListResponsePayload.AuctionDTO, Integer> colAuctionId;
    @FXML private TableColumn<AuctionListResponsePayload.AuctionDTO, String> colItemName;
    @FXML private TableColumn<AuctionListResponsePayload.AuctionDTO, String> colStatus;
    @FXML private TableColumn<AuctionListResponsePayload.AuctionDTO, AuctionListResponsePayload.AuctionDTO> colAuctionAction;
    @FXML private Button btnAllAuctions;
    @FXML private Button btnOpenAuctions;
    @FXML private Button btnRunningAuctions;
    @FXML private Button btnSuccessfulSales;
    @FXML private Button btnCancelledAuctions;
    private List<AuctionListResponsePayload.AuctionDTO> allAuctionsList;

    // System Log fields
    @FXML private javafx.scene.control.TextArea txtSystemLogs;
    private boolean isSubscribedToLogs = false;

    @FXML
    public void initialize() {
        registerEventHandlers();
        ControllerLifecycle.unsubscribeOnDetach(tableUsers, this);
        setupUserTable();
        setupAuctionTable();
        
        btnAllAuctions.setOnAction(e -> {
            if (allAuctionsList != null) tableAuctions.setItems(FXCollections.observableArrayList(allAuctionsList));
        });
        
        btnOpenAuctions.setOnAction(e -> {
            if (allAuctionsList != null) {
                List<AuctionListResponsePayload.AuctionDTO> filtered = allAuctionsList.stream()
                    .filter(a -> "OPEN".equals(a.status))
                    .collect(java.util.stream.Collectors.toList());
                tableAuctions.setItems(FXCollections.observableArrayList(filtered));
            }
        });
        
        btnRunningAuctions.setOnAction(e -> {
            if (allAuctionsList != null) {
                List<AuctionListResponsePayload.AuctionDTO> filtered = allAuctionsList.stream()
                    .filter(a -> "RUNNING".equals(a.status))
                    .collect(java.util.stream.Collectors.toList());
                tableAuctions.setItems(FXCollections.observableArrayList(filtered));
            }
        });
        
        btnSuccessfulSales.setOnAction(e -> {
            if (allAuctionsList != null) {
                List<AuctionListResponsePayload.AuctionDTO> filtered = allAuctionsList.stream()
                    .filter(a -> "FINISHED".equals(a.status) || "PAID".equals(a.status))
                    .collect(java.util.stream.Collectors.toList());
                tableAuctions.setItems(FXCollections.observableArrayList(filtered));
            }
        });

        btnCancelledAuctions.setOnAction(e -> {
            if (allAuctionsList != null) {
                List<AuctionListResponsePayload.AuctionDTO> filtered = allAuctionsList.stream()
                    .filter(a -> "CANCELLED".equals(a.status))
                    .collect(java.util.stream.Collectors.toList());
                tableAuctions.setItems(FXCollections.observableArrayList(filtered));
            }
        });

        loadUserData();
        loadAuctionData();
        
        if (!isSubscribedToLogs) {
            subscribeSystemLogs();
            isSubscribedToLogs = true;
        }
    }

    private void registerEventHandlers() {
        ClientEventBus eventBus = ClientEventBus.getDefault();
        eventBus.subscribe(ClientEvents.UsersLoaded.class, this, AdminDashboardController::handleUsersLoaded);
        eventBus.subscribe(ClientEvents.AllAuctionsLoaded.class, this, AdminDashboardController::handleAllAuctionsLoaded);
        eventBus.subscribe(ClientEvents.CancelAuctionResult.class, this, AdminDashboardController::handleCancelAuctionEvent);
        eventBus.subscribe(ClientEvents.UserDeleted.class, this, AdminDashboardController::handleUserDeleted);
        eventBus.subscribe(ClientEvents.SystemLogsLoaded.class, this, AdminDashboardController::handleSystemLogsLoaded);
    }

    // Removed activity log methods

    private void subscribeSystemLogs() {
        try {
            ServerConnection.getInstance().sendMessage(new Packet(PacketType.SUBSCRIBE_SYSTEM_LOGS, new com.nhom3.shared.network.payload.SystemLogSubscribePayload(true)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    public void handleSystemLogsResult(String logs, boolean isAppend) {
        if (isAppend) {
            txtSystemLogs.appendText(logs);
        } else {
            txtSystemLogs.setText(logs);
        }
    }

    private void handleSystemLogsLoaded(ClientEvents.SystemLogsLoaded event) {
        handleSystemLogsResult(event.logs(), event.isAppend());
    }

    // Duplicates removed

    public void handleLoadUserDataResult(List<UserListResponsePayload.UserDTO> users) {
        tableUsers.setItems(FXCollections.observableArrayList(users));
        txtTotalUsers.setText(String.format("%,d", users == null ? 0 : users.size()));
    }

    private void handleUsersLoaded(ClientEvents.UsersLoaded event) {
        handleLoadUserDataResult(event.users());
    }

    public void handleLoadAuctionDataResult(List<AuctionListResponsePayload.AuctionDTO> auctions) {
        this.allAuctionsList = auctions;
        tableAuctions.setItems(FXCollections.observableArrayList(auctions));
        int count = 0;
        double totalValue = 0;
        if (auctions != null) {
            for (AuctionListResponsePayload.AuctionDTO auction : auctions) {
                if ("RUNNING".equals(auction.status) || "OPEN".equals(auction.status)) {
                    count++;
                }
                if ("PAID".equals(auction.status)) {
                    totalValue += auction.curHighest;
                }
            }
        }
        txtActiveAuctions.setText(String.format("%,d", count));
        txtTotalValue.setText(String.format("%,.0f", totalValue));
    }

    private void handleAllAuctionsLoaded(ClientEvents.AllAuctionsLoaded event) {
        handleLoadAuctionDataResult(event.auctions());
    }

    public void handleCancelAuctionResult(boolean success, String message) {
        showAlert(success ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR,
                success ? "Thành Công" : "Thất Bại", message);
        if (success) {
            loadAuctionData();
        }
    }

    private void handleCancelAuctionEvent(ClientEvents.CancelAuctionResult event) {
        handleCancelAuctionResult(event.success(), event.message());
    }

    public void handleDeleteUserResult(boolean success, String message) {
        showAlert(success ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR,
                success ? "Thành Công" : "Thất Bại", message);
        if (success) {
            loadUserData();
        }
    }

    private void handleUserDeleted(ClientEvents.UserDeleted event) {
        handleDeleteUserResult(event.success(), event.message());
    }

    private void setupUserTable() {
        colUserId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().id).asObject());
        colUserName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().username));
        colUserRole.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().role));
        colUserAction.setCellValueFactory(param -> new javafx.beans.property.SimpleObjectProperty<>(param.getValue()));
        colUserAction.setCellFactory(column -> new TableCell<UserListResponsePayload.UserDTO, UserListResponsePayload.UserDTO>() {
            private final Button btnInfo = new Button("Xem Thông Tin");
            private final Button btnDelete = new Button("Xóa Người Dùng");
            private final javafx.scene.layout.HBox pane = new javafx.scene.layout.HBox(
                    USER_ACTION_BUTTON_SPACING, btnInfo, btnDelete);

            {
                btnInfo.setOnAction(event -> {
                    UserListResponsePayload.UserDTO user = getItem();
                    if (user != null) {
                        showAlert(Alert.AlertType.INFORMATION, "Thông Tin Người Dùng",
                                "Tên: " + user.fullName + "\nEmail: " + user.email + "\nPhone: " + user.phone);
                    }
                });

                btnDelete.setStyle("-fx-background-color: #ff4d4d; -fx-text-fill: white;");
                btnDelete.setOnAction(event -> {
                    UserListResponsePayload.UserDTO user = getItem();
                    if (user == null) return;
                    
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Xác Nhận Xóa");
                    confirm.setHeaderText(null);
                    confirm.setContentText("Bạn có chắc chắn muốn xóa tài khoản '" + user.username + "' không?");
                    DialogUtils.initOwner(confirm, tableUsers);
                    
                    if (confirm.showAndWait().orElse(javafx.scene.control.ButtonType.CANCEL) == javafx.scene.control.ButtonType.OK) {
                        try {
                            ServerConnection.getInstance().sendMessage(
                                new Packet(PacketType.DELETE_USER, new com.nhom3.shared.network.payload.UserIdPayload(user.id))
                            );
                        } catch (Exception e) {
                            e.printStackTrace();
                            showAlert(Alert.AlertType.ERROR, "Lỗi mạng", "Không thể gửi yêu cầu xóa tài khoản!");
                        }
                    }
                });
            }

            @Override
            protected void updateItem(UserListResponsePayload.UserDTO user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setGraphic(null);
                    return;
                }
                // Vô hiệu hóa nút xóa nếu là tài khoản ADMIN hiện tại
                btnDelete.setDisable("ADMIN".equals(user.role));
                setGraphic(pane);
            }
        });
    }

    private void setupAuctionTable() {
        colAuctionId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().auctionId).asObject());
        colItemName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().itemName));
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().status));
        colAuctionAction.setCellValueFactory(param -> new javafx.beans.property.SimpleObjectProperty<>(param.getValue()));
        colAuctionAction.setCellFactory(column -> new TableCell<AuctionListResponsePayload.AuctionDTO, AuctionListResponsePayload.AuctionDTO>() {
            private final Button btnCancel = new Button("Hủy Phiên");

            {
                btnCancel.setOnAction(event -> {
                    AuctionListResponsePayload.AuctionDTO auction = getItem();
                    if (auction == null) return;
                    try {
                        ServerConnection.getInstance().sendMessage(
                                new Packet(PacketType.CANCEL_AUCTION, new AuctionIdPayload(auction.auctionId)));
                    } catch (Exception e) {
                        e.printStackTrace();
                        showAlert(Alert.AlertType.ERROR, "Lỗi mạng", "Không thể gửi yêu cầu hủy phiên!");
                    }
                });
            }

            @Override
            protected void updateItem(AuctionListResponsePayload.AuctionDTO auction, boolean empty) {
                super.updateItem(auction, empty);
                if (empty || auction == null) {
                    setGraphic(null);
                    return;
                }
                btnCancel.setDisable("FINISHED".equals(auction.status)
                        || "PAID".equals(auction.status)
                        || "CANCELLED".equals(auction.status));
                setGraphic(btnCancel);
            }
        });
    }

    private void loadUserData() {
        try {
            ServerConnection.getInstance().sendMessage(new Packet(PacketType.LOAD_USERS, null));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadAuctionData() {
        try {
            ServerConnection.getInstance().sendMessage(new Packet(PacketType.LOAD_ALL_AUCTIONS, null));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        DialogUtils.initOwner(alert, tableUsers);
        alert.showAndWait();
    }
}
