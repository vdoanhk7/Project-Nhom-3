package com.nhom3.client.controller;

import com.nhom3.shared.network.payload.BidHistoryResponsePayload;
import com.nhom3.shared.model.item.Item;
import com.nhom3.shared.model.user.Bidder;
import com.nhom3.shared.model.user.User;
import com.nhom3.client.utils.MoneyInputFormatter;
import com.nhom3.client.utils.UserSession;
import com.nhom3.shared.model.auction.Auction;
import com.nhom3.shared.model.auction.BidTransaction;
import com.nhom3.shared.network.packet.Packet;
import com.nhom3.shared.network.packet.PacketType;
import com.nhom3.shared.network.payload.ItemActionPayload;
import com.nhom3.shared.network.payload.ItemImagePayload;
import com.nhom3.shared.network.payload.AuctionSubscribePayload;
import com.nhom3.shared.network.payload.BidPayload;
import com.nhom3.client.network.ServerConnection;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
        value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD",
        justification = "JavaFX controllers are reached from the socket dispatcher through the active screen instance.")
public class ViewItemDetailController {

    @FXML private Label lblItemName, lblItemType, lblStatusBadge, lblNoImage;
    @FXML private Label lblCurrentPrice, lblStartPrice, lblBidStep;
    @FXML private Label lblTimeTitle, lblCountdown, lblTimeRange;
    
    @FXML private HBox boxSellerActions; 
    
    // ĐÃ FIX: Chuyển thành VBox để khớp với FXML thiết kế mới
    @FXML private VBox boxBidderActions; 
    // ĐÃ FIX: Thêm ID dòng ngang để nhét nút Auto-bid vào cạnh nút Đặt giá
    @FXML private HBox boxBidInputRow; 
    
    @FXML private TextField txtBidAmount; 
    @FXML private TextField txtBidNote; 
    @FXML private Button btnPlaceBid;     

    @FXML private LineChart<String, Number> priceHistoryChart;
    @FXML private Button btnShowChart;
    @FXML private ImageView imgItem;
    private XYChart.Series<String, Number> priceSeries; // Biến giữ dữ liệu đường giá

    @FXML private TableView<BidTransaction> tableBids;
    @FXML private TableColumn<BidTransaction, String> colBidder;
    @FXML private TableColumn<BidTransaction, String> colBidAmount;
    @FXML private TableColumn<BidTransaction, String> colBidTime;
    @FXML private TableColumn<BidTransaction, String> colBidNote;

    private Timeline countdownTimeline;
    private Auction currentAuction;
    private Item currentItem;
    private int currentBidCount = -1;
    private static ViewItemDetailController instance;
    private double pendingBidAmount = 0; 
    private javafx.scene.layout.VBox autoBidInfoBox; 
    
    private void refreshState() {
        if (currentAuction == null) return;
        
        LocalDateTime now = LocalDateTime.now();
        
        if (now.isBefore(currentAuction.getStartTime())) {
            setupOpenState(currentAuction);
        } else if (now.isBefore(currentAuction.getEndTime())) {
            setupRunningState(currentAuction);
        } else {
            setupFinishedState(currentAuction);
        }
    }
    
    public static ViewItemDetailController getInstance() {
        return instance;
    }

    @FXML
    public void initialize() {
        instance = this; 
        
        colBidder.setCellValueFactory(cellData -> {
            try {
                String name = cellData.getValue().getBidder().getUserInfo().getName();
                return new SimpleStringProperty(name != null ? name : "Ẩn danh");
            } catch (Exception e) {
                return new SimpleStringProperty("Lỗi tên");
            }
        });

        colBidAmount.setCellValueFactory(cellData -> 
            new SimpleStringProperty(String.format("%,.0f VNĐ", cellData.getValue().getAmount()))
        );

        colBidTime.setCellValueFactory(cellData -> {
            try {
                LocalDateTime time = cellData.getValue().getBidTime();
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss - dd/MM/yyyy");
                return new SimpleStringProperty(time.format(formatter));
            } catch (Exception e) {
                return new SimpleStringProperty("Lỗi thời gian");
            }
        });
        
        colBidNote.setCellValueFactory(cellData -> {
            try {
                String note = cellData.getValue().getNote();
                return new SimpleStringProperty(note != null ? note : "");
            } catch (Exception e) {
                return new SimpleStringProperty("");
            }
        });
        
        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Diễn biến giá (VNĐ)");
        priceHistoryChart.getData().add(priceSeries);

        // Tắt hiệu ứng animation để biểu đồ vẽ ngay lập tức, không bị giật/chậm
        priceHistoryChart.setAnimated(false);

        priceHistoryChart.setVisible(false);
        priceHistoryChart.setManaged(false);

        MoneyInputFormatter.install(txtBidAmount);

        // HIỆU ỨNG THẨM MỸ: Hover nút Đặt Giá
        if(btnPlaceBid != null) {
            btnPlaceBid.setOnMouseEntered(e -> btnPlaceBid.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15px; -fx-background-radius: 5; -fx-cursor: hand;"));
            btnPlaceBid.setOnMouseExited(e -> btnPlaceBid.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15px; -fx-background-radius: 5; -fx-cursor: hand;"));
        }
    }

    public void setItemData(Item item, Auction auction, String status) {
        this.currentItem = item;
        lblItemName.setText(item.getName());
        lblItemType.setText("Phân loại: " + item.getType());
        lblStartPrice.setText(String.format("Khởi điểm: %,.0f VNĐ", item.getStartPrice()));
        lblCurrentPrice.setText(String.format("%,.0f VNĐ", item.getCurHighest()));
        this.currentAuction = auction;
        
        if (item.getImageBase64() != null && !item.getImageBase64().isEmpty()) {
            renderItemImage(item.getImageBase64());
        } else {
            showNoImagePlaceholder("Đang tải...");
            requestItemImage(item.getId());
        }
        
        if (auction == null) {
            lblBidStep.setText("Bước giá: --");
            lblStatusBadge.setText("CHƯA ĐĂNG BÁN");
            lblStatusBadge.setStyle("-fx-background-color: #95a5a6; -fx-padding: 5 15; -fx-background-radius: 20; -fx-text-fill: white;");
            lblCountdown.setText("--:--:--");
            lblTimeTitle.setText("THỜI GIAN:");
            lblTimeRange.setText("");
            return;
        }
        
        lblBidStep.setText("Bước giá: " + formatMoney(auction.getBidStep()));
        updateBidInputHint();
        
        if ("PAID".equals(status)) {
            setupPaidState(auction);
        } else if ("CANCELLED".equals(status)) {
            setupCancelledState(auction);
        } else {
            refreshState();
        }
        
        setupDynamicUI();
        loadBidHistory();
        subscribeToAuction(true);
        
        javafx.application.Platform.runLater(() -> {
            if (lblItemName.getScene() != null && lblItemName.getScene().getWindow() != null) {
                javafx.stage.Stage stage = (javafx.stage.Stage) lblItemName.getScene().getWindow();
                stage.setOnCloseRequest(event -> {
                    subscribeToAuction(false);
                    if (countdownTimeline != null) countdownTimeline.stop();
                });
            }
        });
    }

    public void handleItemImageResult(ItemImagePayload payload) {
        if (payload == null || currentItem == null || payload.getItemId() != currentItem.getId()) {
            return;
        }

        String imageBase64 = payload.getImageBase64();
        if (imageBase64 == null || imageBase64.isEmpty()) {
            showNoImagePlaceholder("No Image");
            return;
        }

        currentItem.setImageBase64(imageBase64);
        renderItemImage(imageBase64);
    }

    private void requestItemImage(int itemId) {
        if (itemId <= 0) {
            return;
        }

        Thread thread = new Thread(() -> {
            try {
                ServerConnection.getInstance().sendMessage(
                        new Packet(PacketType.LOAD_ITEM_IMAGE, new ItemActionPayload(itemId, 0)));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "Detail-ImageLoader");
        thread.setDaemon(true);
        thread.start();
    }

    private void renderItemImage(String imageBase64) {
        try {
            byte[] imageBytes = java.util.Base64.getDecoder().decode(imageBase64);
            imgItem.setImage(new Image(new java.io.ByteArrayInputStream(imageBytes)));
            imgItem.setVisible(true);
            if (lblNoImage != null) {
                lblNoImage.setVisible(false);
            }
        } catch (Exception e) {
            showNoImagePlaceholder("No Image");
            e.printStackTrace();
        }
    }

    private void showNoImagePlaceholder(String text) {
        imgItem.setImage(null);
        imgItem.setVisible(false);
        if (lblNoImage != null) {
            lblNoImage.setText(text);
            lblNoImage.setVisible(true);
        }
    }

    private void setupOpenState(Auction auction) {
        lblStatusBadge.setText(" SẮP DIỄN RA ");
        lblStatusBadge.setStyle("-fx-background-color: #f1c40f; -fx-text-fill: black; -fx-padding: 5 15; -fx-background-radius: 20;");
        lblTimeTitle.setText("BẮT ĐẦU SAU:");
        lblTimeRange.setText("Lên sàn lúc: " + formatTime(auction.getStartTime()));
        startCountdown(auction.getStartTime());
    }

    private void setupRunningState(Auction auction) {
        lblStatusBadge.setText(" ĐANG ĐẤU GIÁ ");
        lblStatusBadge.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-padding: 5 15; -fx-background-radius: 20;");
        lblTimeTitle.setText("THỜI GIAN CÒN LẠI:");
        lblTimeRange.setText("Kết thúc lúc: " + formatTime(auction.getEndTime()));
        startCountdown(auction.getEndTime());
    }

    private void setupFinishedState(Auction auction) {
        lblStatusBadge.setText(" ĐÃ KẾT THÚC ");
        lblStatusBadge.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-padding: 5 15; -fx-background-radius: 20;");
        lblTimeTitle.setText("PHIÊN ĐÃ ĐÓNG");
        lblCountdown.setText("00:00:00");
        lblTimeRange.setText("Đã đóng lúc: " + formatTime(auction.getEndTime()));
        if (countdownTimeline != null) countdownTimeline.stop();
    }

    private void setupPaidState(Auction auction) {
        lblStatusBadge.setText("ĐÃ THANH TOÁN");
        lblStatusBadge.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-padding: 5 15; -fx-background-radius: 20;");
        lblTimeTitle.setText("GIAO DỊCH HOÀN TẤT");
        lblCountdown.setText("DONE");
        lblTimeRange.setText("Kết thúc lúc: " + formatTime(auction.getEndTime()));
        if (countdownTimeline != null) countdownTimeline.stop();
    }

    private void setupCancelledState(Auction auction) {
        lblStatusBadge.setText("ĐÃ HỦY BỎ");
        lblStatusBadge.setStyle("-fx-background-color: #7f8c8d; -fx-text-fill: white; -fx-padding: 5 15; -fx-background-radius: 20;");
        lblTimeTitle.setText("PHIÊN ĐÃ ĐÓNG");
        lblCountdown.setText("--:--:--");
        lblTimeRange.setText("Lý do: Theo quy định hệ thống");
        if (countdownTimeline != null) countdownTimeline.stop();
    }

    private void startCountdown(LocalDateTime targetTime) {
        if (countdownTimeline != null) countdownTimeline.stop();

        if (!updateCountdownLabel(targetTime)) return;
        countdownTimeline = new Timeline(
                new KeyFrame(Duration.seconds(1), event -> updateCountdownLabel(targetTime)));
        countdownTimeline.setCycleCount(Animation.INDEFINITE);
        countdownTimeline.play();
    }

    private boolean updateCountdownLabel(LocalDateTime targetTime) {
        long secondsDiff = ChronoUnit.SECONDS.between(LocalDateTime.now(), targetTime);
        if (secondsDiff <= 0) {
            if (countdownTimeline != null) countdownTimeline.stop();
            refreshState(); 
            return false;
        }

        long hours = secondsDiff / 3600;
        long minutes = (secondsDiff % 3600) / 60;
        long seconds = secondsDiff % 60;
        lblCountdown.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
        return true;
    }

    @FXML
    private void handleClose() {
        subscribeToAuction(false);
        if (countdownTimeline != null) countdownTimeline.stop();
        ((Stage) lblItemName.getScene().getWindow()).close();
    }

    private String formatTime(LocalDateTime time) {
        return time.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm - dd/MM"));
    }

    private void setupDynamicUI() {
        if (boxBidderActions == null) return;
        boxBidderActions.setVisible(false); boxBidderActions.setManaged(false);

        User currentUser = UserSession.getInstance().getLoggedInUser();
        if (currentUser == null || currentAuction == null) return;

        if (currentUser instanceof Bidder && currentAuction.getStatus() == com.nhom3.shared.model.auction.StatusOfAuction.RUNNING) {
            boxBidderActions.setVisible(true);
            boxBidderActions.setManaged(true);
            
            // Nhét nút Auto-bid vào Dòng ngang boxBidInputRow
            if (boxBidInputRow != null && boxBidInputRow.getChildren().stream().noneMatch(n -> "btnAutoBid".equals(n.getId()))) {
                Button btnAutoBid = new Button("🤖 Auto-Bid");
                btnAutoBid.setId("btnAutoBid");
                
                String styleNormal = "-fx-background-color: #8e44ad; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15px; -fx-cursor: hand; -fx-background-radius: 5;";
                String styleHover = "-fx-background-color: #9b59b6; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15px; -fx-cursor: hand; -fx-background-radius: 5;";
                
                btnAutoBid.setStyle(styleNormal);
                btnAutoBid.setPrefHeight(45.0); 
                btnAutoBid.setPrefWidth(120.0);
                
                btnAutoBid.setOnMouseEntered(e -> btnAutoBid.setStyle(styleHover));
                btnAutoBid.setOnMouseExited(e -> btnAutoBid.setStyle(styleNormal));
                
                btnAutoBid.setOnAction(e -> openAutoBidDialog());
                boxBidInputRow.getChildren().add(btnAutoBid);
            }
            
            checkAutoBidStatus();
        }
    }

    private void openAutoBidDialog() {
        User currentUser = UserSession.getInstance().getLoggedInUser();
        
        Dialog<com.nhom3.shared.network.payload.AutoBidPayload> dialog = new Dialog<>();
        dialog.setTitle("Cài đặt Đấu giá tự động (Auto-Bid)");
        double sellerBidStep = currentAuction.getBidStep();
        dialog.setHeaderText("Bước giá tự động phải từ " + formatMoney(sellerBidStep)
                + " trở lên và không vượt quá giới hạn giá cao nhất.");

        ButtonType btnSetup = new ButtonType("Lưu Cài Đặt", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSetup, ButtonType.CANCEL);

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(15); grid.setVgap(15);
        grid.setPadding(new javafx.geometry.Insets(20, 50, 10, 10));

        TextField txtMaxAmount = new TextField();
        txtMaxAmount.setPromptText("VD: 20000000");
        txtMaxAmount.setStyle("-fx-padding: 8; -fx-background-radius: 5;");
        
        TextField txtIncrement = new TextField();
        txtIncrement.setStyle("-fx-padding: 8; -fx-background-radius: 5;");
        
        MoneyInputFormatter.install(txtMaxAmount);
        MoneyInputFormatter.install(txtIncrement);
        txtIncrement.setText(MoneyInputFormatter.formatAmount(sellerBidStep));

        Label l1 = new Label("Giới hạn giá cao nhất (VNĐ):"); l1.setStyle("-fx-font-weight: bold;");
        Label l2 = new Label("Bước giá mỗi lần tự tăng (VNĐ):"); l2.setStyle("-fx-font-weight: bold;");
        Label l3 = new Label("Bước giá seller yêu cầu:"); l3.setStyle("-fx-text-fill: #7f8c8d;");
        Label l4 = new Label(formatMoney(sellerBidStep)); l4.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");

        grid.add(l1, 0, 0); grid.add(txtMaxAmount, 1, 0);
        grid.add(l2, 0, 1); grid.add(txtIncrement, 1, 1);
        grid.add(l3, 0, 2); grid.add(l4, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnSetup) {
                try {
                    double maxAmt = MoneyInputFormatter.parseAmount(txtMaxAmount.getText());
                    double incAmt = MoneyInputFormatter.parseAmount(txtIncrement.getText());
                    
                    if (maxAmt <= currentAuction.getItem().getCurHighest()) {
                        showAlert(Alert.AlertType.ERROR, "Lỗi", "Giá tối đa phải lớn hơn giá hiện tại!");
                        return null;
                    }
                    if (incAmt < sellerBidStep) {
                        showAlert(Alert.AlertType.ERROR, "Lỗi",
                                "Bước giá Auto-Bid phải lớn hơn hoặc bằng " + formatMoney(sellerBidStep) + "!");
                        return null;
                    }
                    return new com.nhom3.shared.network.payload.AutoBidPayload(currentUser.getId(), currentAuction.getId(), maxAmt, incAmt);
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Lỗi nhập liệu", "Vui lòng chỉ nhập số!");
                    return null;
                }
            }
            return null;
        });

        // Gửi qua mạng nếu có dữ liệu hợp lệ
        dialog.showAndWait().ifPresent(payload -> {
            try {
                Packet packet = new Packet(PacketType.PLACE_AUTO_BID, payload);
                ServerConnection.getInstance().sendMessage(packet);
                System.out.println("[Client] Đã gửi yêu cầu Auto-Bid: Max=" + payload.getMaxAmount());
                
                // Đợi Server xử lý rồi tự động gọi giao diện cập nhật (Không gọi showAlert thủ công để tránh popup đúp)
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void handlePlaceBid() {
        User currentUser = UserSession.getInstance().getLoggedInUser();
        if (!(currentUser instanceof Bidder)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi phân quyền", "Chỉ có Người mua (Bidder) mới được phép tham gia trả giá!");
            return;
        }
        
        String input = txtBidAmount.getText().trim();
        if (input.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng nhập số tiền bạn muốn trả!");
            return;
        }
        
        try {
            double bidAmount = MoneyInputFormatter.parseAmount(input);
            double currentHighest = currentAuction.getItem().getCurHighest();  
            double minStep = currentAuction.getBidStep();
            double validMinPrice = currentHighest + minStep;
            
            if (bidAmount < validMinPrice) {
                showAlert(Alert.AlertType.ERROR, "Giá không hợp lệ", 
                    "Số tiền trả giá phải lớn hơn hoặc bằng " + String.format("%,.0f VNĐ", validMinPrice) + 
                    "\n(Bao gồm giá hiện tại + bước giá " + formatMoney(minStep) + ")");
                return;
            }

            this.pendingBidAmount = bidAmount; 
            
            BidPayload payload = new BidPayload(currentUser.getId(), bidAmount, currentAuction.getId());
            Packet packet = new Packet(PacketType.PLACE_BID, payload);
            ServerConnection.getInstance().sendMessage(packet);
            
            System.out.println("[Client] Đã gửi yêu cầu đặt giá: " + bidAmount);

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi nhập liệu", "Vui lòng chỉ nhập số tiền hợp lệ (VD: 5500000 hoặc 5.500.000)");
        } catch (Exception e) {
            e.printStackTrace(); 
            showAlert(Alert.AlertType.ERROR, "Lỗi mạng", "Không thể gửi yêu cầu đặt giá.");
        }
    }

    @FXML
    private void handleToggleChart() {
        boolean isShowing = priceHistoryChart.isVisible();
        if (isShowing) {
            priceHistoryChart.setVisible(false);
            priceHistoryChart.setManaged(false);
            btnShowChart.setText("📊 Xem Biểu Đồ Giá");
            btnShowChart.setStyle("-fx-background-color: #E0F2FE; -fx-text-fill: #0284C7; -fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 6 15;");
        } else {
            priceHistoryChart.setVisible(true);
            priceHistoryChart.setManaged(true);
            btnShowChart.setText("❌ Đóng Biểu Đồ");
            btnShowChart.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #DC2626; -fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 6 15;");

            refreshChartFromBidList(tableBids.getItems());
            loadBidHistory();
        }
    }

    private void loadPriceHistoryToChart() {
        if (currentAuction == null) return;
        refreshChartFromBidList(tableBids.getItems());
    }

    public void handleBidResult(boolean isSuccess, String message) {
        if (isSuccess) {
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Bạn đã đặt giá " + String.format("%,.0f VNĐ", pendingBidAmount) + " thành công!");
            currentAuction.getItem().setCurHighest(pendingBidAmount);
            lblCurrentPrice.setText(String.format("%,.0f VNĐ", pendingBidAmount));
            updateBidInputHint();
            txtBidAmount.clear();    
            txtBidNote.clear();
            txtBidAmount.requestFocus();
        } else {
            showAlert(Alert.AlertType.ERROR, "Thất bại", message);
        }
    }

    private void loadBidHistory() {
        if (currentAuction == null) return;
        
        com.nhom3.shared.network.payload.AuctionIdPayload payload = new com.nhom3.shared.network.payload.AuctionIdPayload(currentAuction.getId());
        com.nhom3.shared.network.packet.Packet packet = new com.nhom3.shared.network.packet.Packet(com.nhom3.shared.network.packet.PacketType.LOAD_BID_HISTORY, payload);
        
        try {
            com.nhom3.client.network.ServerConnection.getInstance().sendMessage(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        if (lblItemName != null && lblItemName.getScene() != null) {
            Stage stage = (Stage) lblItemName.getScene().getWindow();
            alert.initOwner(stage);
        }
        alert.showAndWait();
    }
    
    public void handleLoadHistoryResult(int responseAuctionId,List<BidHistoryResponsePayload.SimpleBid> simpleList) {
        if (simpleList == null || currentAuction == null) return;
        if (currentAuction.getId() != responseAuctionId) {
            return; 
        }

        List<BidTransaction> realList = new java.util.ArrayList<>();
        for (com.nhom3.shared.network.payload.BidHistoryResponsePayload.SimpleBid sb : simpleList) {
            com.nhom3.shared.model.user.UserInfo info = new com.nhom3.shared.model.user.UserInfo("", "", sb.bidderName != null ? sb.bidderName : "Ẩn danh");
            com.nhom3.shared.model.user.Bidder fakeBidder = new com.nhom3.shared.model.user.Bidder(0, info, null);
            
            java.time.LocalDateTime realTime;
            try {
                realTime = java.time.LocalDateTime.parse(sb.timeStr);
            } catch (Exception e) {
                realTime = java.time.LocalDateTime.now();
            }
            
            String safeNote = sb.note != null ? sb.note : "";
            BidTransaction bid = new BidTransaction(0, fakeBidder, sb.amount, realTime, safeNote);
            realList.add(bid);
        }

        javafx.application.Platform.runLater(() -> {
            javafx.collections.ObservableList<BidTransaction> oList = javafx.collections.FXCollections.observableArrayList(realList);
            tableBids.setItems(oList);
            tableBids.refresh(); 

            if (!realList.isEmpty()) {
                double realHighest = realList.get(0).getAmount();
                if (realHighest > currentAuction.getItem().getCurHighest()) {
                    currentAuction.getItem().setCurHighest(realHighest);
                    lblCurrentPrice.setText(String.format("%,.0f VNĐ", realHighest));
                    updateBidInputHint();
                }
            }
            if (priceHistoryChart.isVisible()) {
                refreshChartFromBidList(realList);
            }
        });
    }

    private void refreshChartFromBidList(List<BidTransaction> history) {
        priceSeries.getData().clear();
        if (history == null) return;

        int maxPoints = 15;
        int startIndex = Math.min(history.size() - 1, maxPoints - 1);

        for (int i = startIndex; i >= 0; i--) {
            BidTransaction bid = history.get(i);
            String time = bid.getBidTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            
            String bidderName = "Ẩn danh";
            if (bid.getBidder() != null && bid.getBidder().getUserInfo() != null && bid.getBidder().getUserInfo().getName() != null) {
                bidderName = bid.getBidder().getUserInfo().getName();
                String[] parts = bidderName.split(" ");
                if (parts.length > 0) {
                    bidderName = parts[parts.length - 1]; 
                }
            }
            
            String category = bidderName + "\n" + time;
            priceSeries.getData().add(new XYChart.Data<>(category, bid.getAmount()));
        }
    }

    public void handleScreenNotify(double highestPrice) {
        if (currentAuction == null) return;

        if (highestPrice > currentAuction.getItem().getCurHighest()) {
            currentAuction.getItem().setCurHighest(highestPrice);
            lblCurrentPrice.setText(String.format("%,.0f VNĐ", highestPrice));
            updateBidInputHint();
        }
        loadBidHistory();
    }

    private void updateBidInputHint() {
        if (currentAuction == null || txtBidAmount == null) return;

        double nextMinBid = currentAuction.getItem().getCurHighest() + currentAuction.getBidStep();
        txtBidAmount.setPromptText("Tối thiểu " + formatMoney(nextMinBid));
    }

    private String formatMoney(double amount) {
        return String.format("%,.0f VNĐ", amount);
    }

    private void subscribeToAuction(boolean subscribe) {
        if (currentAuction == null) return;

        try {
            AuctionSubscribePayload payload = new AuctionSubscribePayload(currentAuction.getId(), subscribe);
            Packet packet = new Packet(PacketType.AUCTION_SUBSCRIBE, payload);
            ServerConnection.getInstance().sendMessage(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void checkAutoBidStatus() {
        User currentUser = UserSession.getInstance().getLoggedInUser();
        if (currentUser == null || currentAuction == null) return;
        
        com.nhom3.shared.network.payload.AutoBidPayload payload = new com.nhom3.shared.network.payload.AutoBidPayload(currentUser.getId(), currentAuction.getId(), 0, 0);
        Packet packet = new Packet(PacketType.CHECK_AUTO_BID, payload);
        try { ServerConnection.getInstance().sendMessage(packet); } catch (Exception e) { e.printStackTrace(); }
    }

    public void handleCheckAutoBidResult(com.nhom3.shared.network.payload.AutoBidPayload config) {
        if (config == null) {
            // CHƯA BẬT AUTO-BID -> Hiện nút đặt tay, ẩn bảng Info
            boxBidderActions.setVisible(true); boxBidderActions.setManaged(true);
            if (autoBidInfoBox != null) { autoBidInfoBox.setVisible(false); autoBidInfoBox.setManaged(false); }
        } else {
            // ĐÃ BẬT AUTO-BID -> Ẩn nút đặt tay, tạo bảng Info
            boxBidderActions.setVisible(false); boxBidderActions.setManaged(false);

            if (autoBidInfoBox == null) {
                autoBidInfoBox = new javafx.scene.layout.VBox(10);
                
                // HIỆU ỨNG THẨM MỸ: Bảng nền Tím
                autoBidInfoBox.setStyle("-fx-background-color: #f5eef8; -fx-padding: 20; -fx-background-radius: 8; -fx-border-color: #9b59b6; -fx-border-width: 2; -fx-border-radius: 8;");
                DropShadow shadow = new DropShadow();
                shadow.setColor(Color.web("#00000020"));
                shadow.setRadius(5); shadow.setOffsetY(3);
                autoBidInfoBox.setEffect(shadow);
                
                Label lblTitle = new Label("🤖 ĐANG BẬT ĐẤU GIÁ TỰ ĐỘNG");
                lblTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #8e44ad; -fx-font-size: 15px;");
                
                Label lblMax = new Label(); lblMax.setId("lblMax");
                lblMax.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 14px; -fx-font-weight: bold;");
                
                Label lblInc = new Label(); lblInc.setId("lblInc");
                lblInc.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 14px; -fx-font-weight: bold;");
                
                javafx.scene.layout.HBox btnBox = new javafx.scene.layout.HBox(15);
                btnBox.setPadding(new javafx.geometry.Insets(10, 0, 0, 0));
                
                Button btnEdit = new Button("Thay đổi thiết lập");
                String editStyle = "-fx-background-color: white; -fx-text-fill: #2c3e50; -fx-border-color: #bdc3c7; -fx-border-radius: 5; -fx-cursor: hand; -fx-background-radius: 5; -fx-padding: 8 20; -fx-font-weight: bold;";
                String editHover = "-fx-background-color: #ecf0f1; -fx-text-fill: #2c3e50; -fx-border-color: #bdc3c7; -fx-border-radius: 5; -fx-cursor: hand; -fx-background-radius: 5; -fx-padding: 8 20; -fx-font-weight: bold;";
                btnEdit.setStyle(editStyle);
                btnEdit.setOnMouseEntered(e -> btnEdit.setStyle(editHover));
                btnEdit.setOnMouseExited(e -> btnEdit.setStyle(editStyle));
                btnEdit.setOnAction(e -> openAutoBidDialog()); 
                
                Button btnCancelAuto = new Button("Dừng Auto-Bid");
                String cancelStyle = "-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5; -fx-padding: 8 20;";
                String cancelHover = "-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5; -fx-padding: 8 20;";
                btnCancelAuto.setStyle(cancelStyle);
                btnCancelAuto.setOnMouseEntered(e -> btnCancelAuto.setStyle(cancelHover));
                btnCancelAuto.setOnMouseExited(e -> btnCancelAuto.setStyle(cancelStyle));
                btnCancelAuto.setOnAction(e -> handleCancelAutoBid()); 
                
                btnBox.getChildren().addAll(btnEdit, btnCancelAuto);
                autoBidInfoBox.getChildren().addAll(lblTitle, lblMax, lblInc, btnBox);
                
                javafx.scene.layout.Pane parent = (javafx.scene.layout.Pane) boxBidderActions.getParent();
                if (!parent.getChildren().contains(autoBidInfoBox)) {
                    parent.getChildren().add(parent.getChildren().indexOf(boxBidderActions), autoBidInfoBox);
                }
            }
            
            Label lblMax = (Label) autoBidInfoBox.lookup("#lblMax");
            Label lblInc = (Label) autoBidInfoBox.lookup("#lblInc");
            lblMax.setText("💵 Giới hạn ví: " + String.format("%,.0f VNĐ", config.getMaxAmount()));
            lblInc.setText("📈 Bước giá tự động: " + String.format("%,.0f VNĐ", config.getIncrement()));
            
            autoBidInfoBox.setVisible(true); autoBidInfoBox.setManaged(true);
        }
    }

    private void handleCancelAutoBid() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Bạn có chắc chắn muốn dừng hệ thống đấu giá tự động?\nSau khi dừng, bạn sẽ phải tự đặt giá bằng tay.", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Xác nhận dừng");
        confirm.setHeaderText(null);
        
        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            User currentUser = UserSession.getInstance().getLoggedInUser();
            if (currentUser == null || currentAuction == null) return;
            
            com.nhom3.shared.network.payload.AutoBidPayload payload = new com.nhom3.shared.network.payload.AutoBidPayload(currentUser.getId(), currentAuction.getId(), 0, 0);
            Packet packet = new Packet(PacketType.CANCEL_AUTO_BID, payload);
            
            try { 
                ServerConnection.getInstance().sendMessage(packet); 
            } catch (Exception e) { 
                e.printStackTrace(); 
            }
        }
    }

}
