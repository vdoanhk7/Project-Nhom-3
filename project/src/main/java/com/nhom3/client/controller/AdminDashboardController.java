package com.nhom3.client.controller;

import com.nhom3.client.network.ServerConnection;
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

@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
        value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD",
        justification = "JavaFX controllers are reached from the socket dispatcher through the active screen instance.")
public class AdminDashboardController {

    @FXML private Text txtTotalUsers;
    @FXML private Text txtActiveAuctions;
    @FXML private Text txtTotalValue;

    @FXML private TableView<UserListResponsePayload.UserDTO> tableUsers;
    @FXML private TableColumn<UserListResponsePayload.UserDTO, Integer> colUserId;
    @FXML private TableColumn<UserListResponsePayload.UserDTO, String> colUserName;
    @FXML private TableColumn<UserListResponsePayload.UserDTO, String> colUserRole;
    @FXML private TableColumn<UserListResponsePayload.UserDTO, Void> colUserAction;

    @FXML private TableView<AuctionListResponsePayload.AuctionDTO> tableAuctions;
    @FXML private TableColumn<AuctionListResponsePayload.AuctionDTO, Integer> colAuctionId;
    @FXML private TableColumn<AuctionListResponsePayload.AuctionDTO, String> colItemName;
    @FXML private TableColumn<AuctionListResponsePayload.AuctionDTO, String> colStatus;
    @FXML private TableColumn<AuctionListResponsePayload.AuctionDTO, Void> colAuctionAction;

    // System Log fields
    @FXML private javafx.scene.control.TextArea txtSystemLogs;

    private static AdminDashboardController instance;

    public static AdminDashboardController getInstance() {
        return instance;
    }

    @FXML
    public void initialize() {
        instance = this;
        setupUserTable();
        setupAuctionTable();
        loadUserData();
        loadAuctionData();
        
        loadSystemLogs();
        javafx.animation.Timeline timeline = new javafx.animation.Timeline(new javafx.animation.KeyFrame(
            javafx.util.Duration.seconds(3),
            ev -> loadSystemLogs()
        ));
        timeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        timeline.play();
    }

    // Removed activity log methods

    private void loadSystemLogs() {
        try {
            ServerConnection.getInstance().sendMessage(new Packet(PacketType.GET_SYSTEM_LOGS, null));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    public void handleSystemLogsResult(String logs) {
        txtSystemLogs.setText(logs);
    }

    // Duplicates removed

    public void handleLoadUserDataResult(List<UserListResponsePayload.UserDTO> users) {
        tableUsers.setItems(FXCollections.observableArrayList(users));
        txtTotalUsers.setText(String.format("%,d", users == null ? 0 : users.size()));
    }

    public void handleLoadAuctionDataResult(List<AuctionListResponsePayload.AuctionDTO> auctions) {
        tableAuctions.setItems(FXCollections.observableArrayList(auctions));
        int count = auctions == null ? 0 : auctions.size();
        double totalValue = 0;
        if (auctions != null) {
            for (AuctionListResponsePayload.AuctionDTO auction : auctions) {
                totalValue += auction.curHighest;
            }
        }
        txtActiveAuctions.setText(String.format("%,d", count));
        txtTotalValue.setText(String.format("%,.0f", totalValue));
    }

    public void handleCancelAuctionResult(boolean success, String message) {
        showAlert(success ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR,
                success ? "Thanh cong" : "That bai", message);
        if (success) {
            loadAuctionData();
        }
    }

    public void handleDeleteUserResult(boolean success, String message) {
        showAlert(success ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR,
                success ? "Thành công" : "Thất bại", message);
        if (success) {
            loadUserData();
        }
    }

    private void setupUserTable() {
        colUserId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().id).asObject());
        colUserName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().username));
        colUserRole.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().role));
        colUserAction.setCellFactory(column -> new TableCell<>() {
            private final Button btnInfo = new Button("Xem");
            private final Button btnDelete = new Button("Xóa");
            private final javafx.scene.layout.HBox pane = new javafx.scene.layout.HBox(5, btnInfo, btnDelete);

            {
                btnInfo.setOnAction(event -> {
                    UserListResponsePayload.UserDTO user = getTableView().getItems().get(getIndex());
                    showAlert(Alert.AlertType.INFORMATION, "Thong tin nguoi dung",
                            user.fullName + "\nEmail: " + user.email + "\nPhone: " + user.phone);
                });

                btnDelete.setStyle("-fx-background-color: #ff4d4d; -fx-text-fill: white;");
                btnDelete.setOnAction(event -> {
                    UserListResponsePayload.UserDTO user = getTableView().getItems().get(getIndex());
                    
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Xác nhận xóa");
                    confirm.setHeaderText(null);
                    confirm.setContentText("Bạn có chắc chắn muốn xóa tài khoản '" + user.username + "' không?");
                    
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
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    UserListResponsePayload.UserDTO user = getTableView().getItems().get(getIndex());
                    // Vô hiệu hóa nút xóa nếu là tài khoản ADMIN hiện tại (hoặc có thể chặn xóa ADMIN nói chung)
                    btnDelete.setDisable("ADMIN".equals(user.role));
                    setGraphic(pane);
                }
            }
        });
    }

    private void setupAuctionTable() {
        colAuctionId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().auctionId).asObject());
        colItemName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().itemName));
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().status));
        colAuctionAction.setCellFactory(column -> new TableCell<>() {
            private final Button btnCancel = new Button("Huy phien");

            {
                btnCancel.setOnAction(event -> {
                    AuctionListResponsePayload.AuctionDTO auction = getTableView().getItems().get(getIndex());
                    try {
                        ServerConnection.getInstance().sendMessage(
                                new Packet(PacketType.CANCEL_AUCTION, new AuctionIdPayload(auction.auctionId)));
                    } catch (Exception e) {
                        e.printStackTrace();
                        showAlert(Alert.AlertType.ERROR, "Loi mang", "Khong the gui yeu cau huy phien!");
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                AuctionListResponsePayload.AuctionDTO auction = getTableView().getItems().get(getIndex());
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
            ServerConnection.getInstance().sendMessage(new Packet(PacketType.LOAD_ACTIVE_AUCTIONS, null));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
