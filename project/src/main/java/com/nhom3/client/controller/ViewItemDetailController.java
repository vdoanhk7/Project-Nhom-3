package com.nhom3.client.controller;

import com.nhom3.shared.network.payload.BidHistoryResponsePayload;
import com.nhom3.shared.model.item.Item;
import com.nhom3.shared.model.user.Bidder;
import com.nhom3.shared.model.user.User;
import com.nhom3.client.utils.UserSession;
import com.nhom3.shared.model.auction.Auction;
import com.nhom3.shared.model.auction.BidTransaction;
import com.nhom3.shared.network.packet.Packet;
import com.nhom3.shared.network.packet.PacketType;
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
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
        value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD",
        justification = "JavaFX controllers are reached from the socket dispatcher through the active screen instance.")
public class ViewItemDetailController {

    @FXML private Label lblItemName, lblItemType, lblStatusBadge;
    @FXML private Label lblCurrentPrice, lblStartPrice;
    @FXML private Label lblTimeTitle, lblCountdown, lblTimeRange;
    @FXML private HBox boxSellerActions; // Vùng chứa nút của Seller (Hiện tại chưa dùng đến)
    @FXML private HBox boxBidderActions; // Vùng chứa ô nhập tiền và nút Đặt giá
    @FXML private TextField txtBidAmount; // Ô nhập số tiền đặt giá
    @FXML private TextField txtBidNote; // Ô nhập ghi chú
    @FXML private Button btnPlaceBid;     // Nút "Đặt giá"

    @FXML private LineChart<String, Number> priceHistoryChart;
    @FXML private Button btnShowChart;
    private XYChart.Series<String, Number> priceSeries; // Biến giữ dữ liệu đường giá

    // Giữ lại khai báo bảng để FXML không bị lỗi, nhưng chưa dùng đến
    @FXML private TableView<BidTransaction> tableBids;
    @FXML private TableColumn<BidTransaction, String> colBidder;
    @FXML private TableColumn<BidTransaction, String> colBidAmount;
    @FXML private TableColumn<BidTransaction, String> colBidTime;
    @FXML private TableColumn<BidTransaction, String> colBidNote;

    private Timeline countdownTimeline;
    private Auction currentAuction;
    private int currentBidCount = -1;
    private static ViewItemDetailController instance;
    private double pendingBidAmount = 0; // Lưu tạm số tiền đang định đặt
    private javafx.scene.layout.VBox autoBidInfoBox; // Hộp giao diện hiển thị thông tin Auto-bid
    private boolean isFormattingBidAmount = false;
    //Tự động cập nhật thời gian
    private void refreshState() {
        if (currentAuction == null) return;
        
        LocalDateTime now = LocalDateTime.now();
        
        // Tự động kiểm tra thời gian thực
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
        // 1. Cột Người đặt
        colBidder.setCellValueFactory(cellData -> {
            try {
                String name = cellData.getValue().getBidder().getUserInfo().getName();
                return new SimpleStringProperty(name != null ? name : "Ẩn danh");
            } catch (Exception e) {
                return new SimpleStringProperty("Lỗi tên");
            }
        });

        // 2. Cột Mức giá: Format tiền tệ 
        colBidAmount.setCellValueFactory(cellData -> 
            new SimpleStringProperty(String.format("%,.0f VNĐ", cellData.getValue().getAmount()))
        );

        // 3. Cột Thời gian: Format giờ ngày
        colBidTime.setCellValueFactory(cellData -> {
            try {
                LocalDateTime time = cellData.getValue().getBidTime();
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss - dd/MM/yyyy");
                return new SimpleStringProperty(time.format(formatter));
            } catch (Exception e) {
                return new SimpleStringProperty("Lỗi thời gian");
            }
        });
        // 4. Cột ghi chú
        colBidNote.setCellValueFactory(cellData -> {
            try {
                String note = cellData.getValue().getNote();
                return new SimpleStringProperty(note != null ? note : "");
            } catch (Exception e) {
                return new SimpleStringProperty("");
            }
        });
        // Khởi tạo series dữ liệu cho biểu đồ
        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Diễn biến giá (VNĐ)");
        priceHistoryChart.getData().add(priceSeries);

        // Mặc định ẩn biểu đồ để giao diện gọn gàng
        priceHistoryChart.setVisible(false);
        priceHistoryChart.setManaged(false);

        setupBidAmountFormatter();
    }

    public void setItemData(Item item, Auction auction, String status) {
        lblItemName.setText(item.getName());
        lblItemType.setText("Phân loại: " + item.getType());
        lblStartPrice.setText(String.format("Khởi điểm: %,.0f VNĐ", item.getStartPrice()));
        lblCurrentPrice.setText(String.format("%,.0f VNĐ", item.getCurHighest()));
        this.currentAuction = auction;
        if (auction == null) {
            lblStatusBadge.setText("CHƯA ĐĂNG BÁN");
            lblStatusBadge.setStyle("-fx-background-color: #95a5a6;");
            lblCountdown.setText("--:--:--");
            lblTimeTitle.setText("THỜI GIAN:");
            lblTimeRange.setText("");
            return;
        }
        if ("PAID".equals(status)) {
            setupPaidState(auction);
        } else if ("CANCELLED".equals(status)) {
            setupCancelledState(auction);
        } else {
            refreshState();}
        
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

    private void setupOpenState(Auction auction) {
        lblStatusBadge.setText(" SẮP DIỄN RA ");
        lblStatusBadge.setStyle("-fx-background-color: #f1c40f; -fx-text-fill: black;");
        lblTimeTitle.setText("BẮT ĐẦU SAU:");
        lblTimeRange.setText("Lên sàn lúc: " + formatTime(auction.getStartTime()));
        startCountdown(auction.getStartTime());
    }

    private void setupRunningState(Auction auction) {
        lblStatusBadge.setText(" ĐANG ĐẤU GIÁ ");
        lblStatusBadge.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        lblTimeTitle.setText("THỜI GIAN CÒN LẠI:");
        lblTimeRange.setText("Kết thúc lúc: " + formatTime(auction.getEndTime()));
        startCountdown(auction.getEndTime());
    }

    private void setupFinishedState(Auction auction) {
        lblStatusBadge.setText(" ĐÃ KẾT THÚC ");
        lblStatusBadge.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
        lblTimeTitle.setText("PHIÊN ĐÃ ĐÓNG");
        lblCountdown.setText("00:00:00");
        lblTimeRange.setText("Đã đóng lúc: " + formatTime(auction.getEndTime()));
        if (countdownTimeline != null) countdownTimeline.stop();
    }

    private void setupPaidState(Auction auction) {
        lblStatusBadge.setText("ĐÃ THANH TOÁN");
        lblStatusBadge.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;"); // Màu xanh lá
        lblTimeTitle.setText("GIAO DỊCH HOÀN TẤT");
        lblCountdown.setText("DONE");
        lblTimeRange.setText("Kết thúc lúc: " + formatTime(auction.getEndTime()));
        if (countdownTimeline != null) countdownTimeline.stop();
    }

    private void setupCancelledState(Auction auction) {
        lblStatusBadge.setText("ĐÃ HỦY BỎ");
        lblStatusBadge.setStyle("-fx-background-color: #7f8c8d; -fx-text-fill: white;"); // Màu xám xám
        lblTimeTitle.setText("PHIÊN ĐÃ ĐÓNG");
        lblCountdown.setText("--:--:--");
        lblTimeRange.setText("Lý do: Theo quy định hệ thống");
        if (countdownTimeline != null) countdownTimeline.stop();
    }

    private void startCountdown(LocalDateTime targetTime) {
        if (countdownTimeline != null) countdownTimeline.stop();

        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            long secondsDiff = ChronoUnit.SECONDS.between(LocalDateTime.now(), targetTime);
            if (secondsDiff <= 0) {
                countdownTimeline.stop();
                refreshState(); // Khi đếm ngược kết thúc, tự động cập nhật trạng thái mới
            } else {
                long hours = secondsDiff / 3600;
                long minutes = (secondsDiff % 3600) / 60;
                long seconds = secondsDiff % 60;
                lblCountdown.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
            }
        }));
        countdownTimeline.setCycleCount(Animation.INDEFINITE);
        countdownTimeline.play();
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

    private void setupBidAmountFormatter() {
        if (txtBidAmount == null) {
            return;
        }

        txtBidAmount.textProperty().addListener((observable, oldValue, newValue) -> {
            if (isFormattingBidAmount) {
                return;
            }

            String digitsOnly = newValue.replaceAll("[^\\d]", "");
            if (digitsOnly.isEmpty()) {
                if (!newValue.isEmpty()) {
                    isFormattingBidAmount = true;
                    txtBidAmount.clear();
                    isFormattingBidAmount = false;
                }
                return;
            }

            String formattedValue = formatWithThousandsSeparator(digitsOnly);
            if (formattedValue.equals(newValue)) {
                return;
            }

            int digitsBeforeCaret = countDigits(newValue.substring(0, Math.min(txtBidAmount.getCaretPosition(), newValue.length())));

            isFormattingBidAmount = true;
            txtBidAmount.setText(formattedValue);
            txtBidAmount.positionCaret(calculateCaretPosition(formattedValue, digitsBeforeCaret));
            isFormattingBidAmount = false;
        });
    }

    private String formatWithThousandsSeparator(String digitsOnly) {
        return String.format("%,d", new BigInteger(digitsOnly));
    }

    private int countDigits(String value) {
        int digitCount = 0;
        for (int i = 0; i < value.length(); i++) {
            if (Character.isDigit(value.charAt(i))) {
                digitCount++;
            }
        }
        return digitCount;
    }

    private int calculateCaretPosition(String formattedValue, int digitCount) {
        if (digitCount <= 0) {
            return 0;
        }

        int seenDigits = 0;
        for (int i = 0; i < formattedValue.length(); i++) {
            if (Character.isDigit(formattedValue.charAt(i))) {
                seenDigits++;
                if (seenDigits == digitCount) {
                    return i + 1;
                }
            }
        }
        return formattedValue.length();
    }

    // QUYẾT ĐỊNH HIỂN THỊ TUỲ VÀO VAI TRÒ
    private void setupDynamicUI() {
        if (boxBidderActions == null) return;
        boxBidderActions.setVisible(false); boxBidderActions.setManaged(false);

        User currentUser = UserSession.getInstance().getLoggedInUser();
        if (currentUser == null || currentAuction == null) return;

        if (currentUser instanceof Bidder && currentAuction.getStatus() == com.nhom3.shared.model.auction.StatusOfAuction.RUNNING) {
            // Hiển thị tạm box đặt tay, sau đó gọi mạng kiểm tra Auto-Bid
            boxBidderActions.setVisible(true);
            boxBidderActions.setManaged(true);
            
            if (boxBidderActions.getChildren().stream().noneMatch(n -> "btnAutoBid".equals(n.getId()))) {
                Button btnAutoBid = new Button("🤖 Cài Đặt Auto-Bid");
                btnAutoBid.setId("btnAutoBid");
                btnAutoBid.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
                btnAutoBid.setOnAction(e -> openAutoBidDialog());
                boxBidderActions.getChildren().add(btnAutoBid);
            }
            
            // Hỏi Server xem có đang bật Auto-Bid không
            checkAutoBidStatus();
        }
    }

    // Mở cửa sổ nhập thông số Auto-Bid
    private void openAutoBidDialog() {
        User currentUser = UserSession.getInstance().getLoggedInUser();
        
        // Tạo cửa sổ Dialog
        Dialog<com.nhom3.shared.network.payload.AutoBidPayload> dialog = new Dialog<>();
        dialog.setTitle("Cài đặt Đấu giá tự động (Auto-Bid)");
        dialog.setHeaderText("Hệ thống sẽ tự động thay mặt bạn trả giá cao hơn\nngười khác (cộng thêm Bước giá) cho đến khi đạt Mức tối đa.");

        // Nút OK và Cancel
        ButtonType btnSetup = new ButtonType("Lưu Cài Đặt", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSetup, ButtonType.CANCEL);

        // Khung nhập liệu
        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField txtMaxAmount = new TextField();
        txtMaxAmount.setPromptText("VD: 20000000");
        TextField txtIncrement = new TextField();
        txtIncrement.setText("50000"); // Bước giá mặc định

        grid.add(new Label("Giới hạn giá cao nhất (VNĐ):"), 0, 0);
        grid.add(txtMaxAmount, 1, 0);
        grid.add(new Label("Bước giá mỗi lần tự tăng (VNĐ):"), 0, 1);
        grid.add(txtIncrement, 1, 1);

        dialog.getDialogPane().setContent(grid);

        // Lấy dữ liệu khi bấm OK
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnSetup) {
                try {
                    double maxAmt = Double.parseDouble(txtMaxAmount.getText().replace(",", ""));
                    double incAmt = Double.parseDouble(txtIncrement.getText().replace(",", ""));
                    
                    if (maxAmt <= currentAuction.getItem().getCurHighest()) {
                        showAlert(Alert.AlertType.ERROR, "Lỗi", "Giá tối đa phải lớn hơn giá hiện tại!");
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
            double bidAmount = Double.parseDouble(input.replace(",", ""));      
            double currentHighest = currentAuction.getItem().getCurHighest();  
            double minStep = 50000; 
            double validMinPrice = currentHighest + minStep;
            
            if (bidAmount < validMinPrice) {
                showAlert(Alert.AlertType.ERROR, "Giá không hợp lệ", 
                    "Số tiền trả giá phải lớn hơn hoặc bằng " + String.format("%,.0f VNĐ", validMinPrice) + 
                    "\n(Bao gồm giá hiện tại + Bước giá 50k)");
                return;
            }

            // --- BẮT ĐẦU PHẦN GỌI MẠNG (THAY THẾ CODE GỌI DB CŨ) ---
            this.pendingBidAmount = bidAmount; // Lưu tạm để nếu thành công thì update UI
            
            // 1. Tạo Gói hàng BidPayload
            BidPayload payload = new BidPayload(
                currentUser.getId(), 
                bidAmount, 
                currentAuction.getId()
            );
            
            // 2. Bọc vào Packet và gửi đi
            Packet packet = new Packet(PacketType.PLACE_BID, payload);
            ServerConnection.getInstance().sendMessage(packet);
            
            System.out.println("[Client] Đã gửi yêu cầu đặt giá: " + bidAmount);

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi nhập liệu", "Vui lòng chỉ nhập các con số (VD: 5500000)");
        } catch (Exception e) {
            e.printStackTrace(); 
            showAlert(Alert.AlertType.ERROR, "Lỗi mạng", "Không thể gửi yêu cầu đặt giá.");
        }
    }

    // Sử lí ẩn hiện biểu đồ giá
    @FXML
    private void handleToggleChart() {
        boolean isShowing = priceHistoryChart.isVisible();
        if (isShowing) {
            priceHistoryChart.setVisible(false);
            priceHistoryChart.setManaged(false);
            btnShowChart.setText("📊 Xem Biểu Đồ Giá");
        } else {
            priceHistoryChart.setVisible(true);
            priceHistoryChart.setManaged(true);
            btnShowChart.setText("❌ Đóng Biểu Đồ");

            // Mỗi lần mở ra, nạp lại lịch sử mới nhất từ server
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
            // Cập nhật giao diện lập tức
            currentAuction.getItem().setCurHighest(pendingBidAmount);
            lblCurrentPrice.setText(String.format("%,.0f VNĐ", pendingBidAmount));
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
        
        // CHẮC CHẮN PACKET TYPE Ở ĐÂY LÀ LOAD_BID_HISTORY
        com.nhom3.shared.network.packet.Packet packet = new com.nhom3.shared.network.packet.Packet(com.nhom3.shared.network.packet.PacketType.LOAD_BID_HISTORY, payload);
        
        try {
            com.nhom3.client.network.ServerConnection.getInstance().sendMessage(packet);
            // THÊM DÒNG NÀY ĐỂ XEM CLIENT CÓ THỰC SỰ GỬI LỜI YÊU CẦU ĐI KHÔNG
            System.out.println("[Client] Đã gửi gói tin xin Lịch sử của Auction: " + currentAuction.getId()); 
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Hàm hỗ trợ hiện thông báo (Popup) cho đẹp 
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

        // In ra Terminal để kiểm tra xem Server gửi về mấy dòng
        System.out.println("[UI] Nhận được " + simpleList.size() + " dòng lịch sử từ Server");

        // 1. Tái tạo danh sách
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

        // 2. ÉP BẢNG PHẢI VẼ LẠI TRÊN LUỒNG GIAO DIỆN
        javafx.application.Platform.runLater(() -> {
            // Khởi tạo ObservableList mới tinh
            javafx.collections.ObservableList<BidTransaction> oList = javafx.collections.FXCollections.observableArrayList(realList);
            
            // Gắn vào bảng và ép làm mới
            tableBids.setItems(oList);
            tableBids.refresh(); 

            // 3. Cập nhật Giá cao nhất trên đỉnh màn hình
            if (!realList.isEmpty()) {
                double realHighest = realList.get(0).getAmount();
                if (realHighest > currentAuction.getItem().getCurHighest()) {
                    currentAuction.getItem().setCurHighest(realHighest);
                    lblCurrentPrice.setText(String.format("%,.0f VNĐ", realHighest));
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

        // Giới hạn chỉ hiển thị 15 cột để to rõ và không bị rối
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
                    bidderName = parts[parts.length - 1]; // Chỉ lấy Tên cuối cùng cho gọn
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
        }
        loadBidHistory();
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

    // Gửi yêu cầu kiểm tra trạng thái
    public void checkAutoBidStatus() {
        User currentUser = UserSession.getInstance().getLoggedInUser();
        if (currentUser == null || currentAuction == null) return;
        
        com.nhom3.shared.network.payload.AutoBidPayload payload = new com.nhom3.shared.network.payload.AutoBidPayload(currentUser.getId(), currentAuction.getId(), 0, 0);
        Packet packet = new Packet(PacketType.CHECK_AUTO_BID, payload);
        try { ServerConnection.getInstance().sendMessage(packet); } catch (Exception e) { e.printStackTrace(); }
    }

    // Server trả về kết quả
    public void handleCheckAutoBidResult(com.nhom3.shared.network.payload.AutoBidPayload config) {
        if (config == null) {
            // CHƯA BẬT AUTO-BID -> Hiện nút đặt tay, ẩn bảng Info
            boxBidderActions.setVisible(true); boxBidderActions.setManaged(true);
            if (autoBidInfoBox != null) { autoBidInfoBox.setVisible(false); autoBidInfoBox.setManaged(false); }
        } else {
            // ĐÃ BẬT AUTO-BID -> Ẩn nút đặt tay, tạo bảng Info
            boxBidderActions.setVisible(false); boxBidderActions.setManaged(false);

            if (autoBidInfoBox == null) {
                autoBidInfoBox = new javafx.scene.layout.VBox(8);
                autoBidInfoBox.setStyle("-fx-background-color: #f3e5f5; -fx-padding: 15; -fx-background-radius: 5; -fx-border-color: #9b59b6; -fx-border-width: 2;");
                
                Label lblTitle = new Label("🤖 ĐANG BẬT ĐẤU GIÁ TỰ ĐỘNG");
                lblTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #8e44ad; -fx-font-size: 14px;");
                
                Label lblMax = new Label(); lblMax.setId("lblMax");
                Label lblInc = new Label(); lblInc.setId("lblInc");
                
                // Khung chứa 2 nút nằm ngang
                javafx.scene.layout.HBox btnBox = new javafx.scene.layout.HBox(10);
                
                Button btnEdit = new Button("Thay đổi thiết lập");
                btnEdit.setStyle("-fx-background-color: #8e44ad; -fx-text-fill: white; -fx-cursor: hand;");
                btnEdit.setOnAction(e -> openAutoBidDialog()); 
                
                Button btnCancelAuto = new Button("Dừng Auto-Bid");
                btnCancelAuto.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
                btnCancelAuto.setOnAction(e -> handleCancelAutoBid()); // GỌI HÀM HỦY
                
                btnBox.getChildren().addAll(btnEdit, btnCancelAuto);
                autoBidInfoBox.getChildren().addAll(lblTitle, lblMax, lblInc, btnBox);
                
                javafx.scene.layout.Pane parent = (javafx.scene.layout.Pane) boxBidderActions.getParent();
                if (!parent.getChildren().contains(autoBidInfoBox)) {
                    parent.getChildren().add(parent.getChildren().indexOf(boxBidderActions), autoBidInfoBox);
                }
            }
            
            // Cập nhật thông số
            Label lblMax = (Label) autoBidInfoBox.lookup("#lblMax");
            Label lblInc = (Label) autoBidInfoBox.lookup("#lblInc");
            lblMax.setText("Giới hạn ví: " + String.format("%,.0f VNĐ", config.getMaxAmount()));
            lblInc.setText("Bước giá tự động: " + String.format("%,.0f VNĐ", config.getIncrement()));
            
            autoBidInfoBox.setVisible(true); autoBidInfoBox.setManaged(true);
        }
    }

    // Xử lý khi bấm nút "Dừng Auto-Bid"
    private void handleCancelAutoBid() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Bạn có chắc chắn muốn dừng hệ thống đấu giá tự động?\nSau khi dừng, bạn sẽ phải tự đặt giá bằng tay.", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Xác nhận dừng");
        confirm.setHeaderText(null);
        
        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            User currentUser = UserSession.getInstance().getLoggedInUser();
            if (currentUser == null || currentAuction == null) return;
            
            // Tái sử dụng AutoBidPayload nhưng truyền maxAmount=0, increment=0 để gửi đi
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
