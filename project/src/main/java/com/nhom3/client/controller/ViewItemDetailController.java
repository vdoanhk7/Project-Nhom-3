package com.nhom3.client.controller;

import com.nhom3.shared.model.item.Item;
import com.nhom3.shared.model.user.Bidder;
import com.nhom3.shared.model.user.Seller;
import com.nhom3.shared.model.user.User;
import com.nhom3.client.utils.UserSession;
import com.nhom3.shared.model.auction.Auction;
import com.nhom3.server.dao.AuctionDAO;
import com.nhom3.server.dao.AuctionDAOImpl;
import com.nhom3.shared.model.auction.BidTransaction;
import com.nhom3.shared.model.auction.StatusOfAuction;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.application.Platform;
import java.util.concurrent.CompletableFuture;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class ViewItemDetailController {

    @FXML private Label lblItemName, lblItemType, lblStatusBadge;
    @FXML private Label lblCurrentPrice, lblStartPrice;
    @FXML private Label lblTimeTitle, lblCountdown, lblTimeRange;
    @FXML private HBox boxSellerActions; // Vùng chứa nút của Seller (Hiện tại chưa dùng đến)
    @FXML private HBox boxBidderActions; // Vùng chứa ô nhập tiền và nút Đặt giá
    @FXML private TextField txtBidAmount; // Ô nhập số tiền đặt giá
    @FXML private TextField txtBidNote; // Ô nhập ghi chú
    @FXML private Button btnPlaceBid;     // Nút "Đặt giá"

    // Giữ lại khai báo bảng để FXML không bị lỗi, nhưng chưa dùng đến
    @FXML private TableView<BidTransaction> tableBids;
    @FXML private TableColumn<BidTransaction, String> colBidder;
    @FXML private TableColumn<BidTransaction, String> colBidAmount;
    @FXML private TableColumn<BidTransaction, String> colBidTime;
    @FXML private TableColumn<BidTransaction, String> colBidNote;

    private Timeline countdownTimeline;
    private Timeline pollingTimeline;
    private Auction currentAuction;
    private int currentBidCount = -1;
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

    @FXML
    public void initialize() {
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
        startPollingData(); 
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
        if (pollingTimeline != null) pollingTimeline.stop();
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
        if (countdownTimeline != null) countdownTimeline.stop();
        if (pollingTimeline != null) pollingTimeline.stop();
        ((Stage) lblItemName.getScene().getWindow()).close();
    }

    private String formatTime(LocalDateTime time) {
        return time.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm - dd/MM"));
    }
    // QUYẾT ĐỊNH HIỂN THỊ TUỲ VÀO VAI TRÒ
    private void setupDynamicUI() {
        if (boxBidderActions == null) return;

        // 1. LUÔN ẨN VÙNG ĐẶT GIÁ TRƯỚC (Để đề phòng)
        boxBidderActions.setVisible(false); 
        boxBidderActions.setManaged(false);

        User currentUser = UserSession.getInstance().getLoggedInUser();
        if (currentUser == null || currentAuction == null) return;

        // 2. PHÂN QUYỀN CHẶT CHẼ
        if (currentUser instanceof Seller) {
            // LÀ SELLER: Không làm gì cả (Giao diện đặt giá đã bị ẩn ở bước 1)
            System.out.println("🔒 Đã khóa giao diện đặt giá của Seller.");
        } 
        else if (currentUser instanceof Bidder) {
            // LÀ BIDDER: Chỉ mở khóa vùng Đặt giá khi phiên đang RUNNING
            if (currentAuction.getStatus() == com.nhom3.shared.model.auction.StatusOfAuction.RUNNING) {
                boxBidderActions.setVisible(true);
                boxBidderActions.setManaged(true);
                System.out.println("🔓 Đã mở khóa ô đặt giá cho Bidder.");
            }
        }
    }

    @FXML
    private void handlePlaceBid() {
        //KHÔNG PHẢI BIDDER KHÔNG THỂ ĐẤU GIÁ
        User currentUser = UserSession.getInstance().getLoggedInUser();
        if (!(currentUser instanceof Bidder)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi phân quyền", "Chỉ có Người mua (Bidder) mới được phép tham gia trả giá!");
            return;
        }
        // 1. Kiểm tra xem người dùng đã nhập gì chưa
        String input = txtBidAmount.getText().trim();
        if (input.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng nhập số tiền bạn muốn trả!");
            return;
        }
        try {
            // 2. Chuyển đổi chuỗi thành số (Loại bỏ dấu phẩy nếu người dùng nhập kiểu 1,000,000)
            double bidAmount = Double.parseDouble(input.replace(",", ""));      
            // 3. Lấy giá cao nhất hiện tại
            double currentHighest = currentAuction.getItem().getCurHighest();  
            // Giả sử bước giá tối thiểu (Minimum Step) là 50,000 VNĐ
            double minStep = 50000; 
            double validMinPrice = currentHighest + minStep;
            // 4. Kiểm tra logic giá
            if (bidAmount < validMinPrice) {
                showAlert(Alert.AlertType.ERROR, "Giá không hợp lệ", 
                    "Số tiền trả giá phải lớn hơn hoặc bằng " + String.format("%,.0f VNĐ", validMinPrice) + 
                    "\n(Bao gồm giá hiện tại + Bước giá 50k)");
                return;
            }

            // 5. Nếu giá hợp lệ -> Bắt đầu ghi vào Database
            Bidder bidder = (Bidder) currentUser; 
        
            AuctionDAO auctionDAO = new AuctionDAOImpl();
            boolean isUpdateSuccess = auctionDAO.updateHighestBid(currentAuction.getId(), bidder.getId(), bidAmount);

            if (isUpdateSuccess) {
                // Tạo đối tượng Lịch sử và Lưu
                String userNote = txtBidNote.getText().trim();
                if (userNote.isEmpty()) {
                    userNote = "Đặt giá qua giao diện";
                }
                BidTransaction newBid = new BidTransaction(0, bidder, bidAmount, LocalDateTime.now(), userNote);
                // Lưu vào DB
                auctionDAO.saveBidTransaction(newBid, currentAuction.getId());
                // Cập nhật Giao diện ngay lập tức
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Bạn đã đặt giá " + String.format("%,.0f VNĐ", bidAmount) + " thành công!");
                currentAuction.getItem().setCurHighest(bidAmount);
                lblCurrentPrice.setText(String.format("%,.0f VNĐ", bidAmount));
                txtBidAmount.clear();    
                txtBidNote.clear();
                txtBidAmount.requestFocus();
                loadBidHistory();
                
            } else {
                showAlert(Alert.AlertType.ERROR, "Thất bại", "Có người khác đã trả giá cao hơn bạn! Vui lòng tải lại trang.");
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi nhập liệu", "Vui lòng chỉ nhập các con số (VD: 5500000)");
        }
    }

    private void loadBidHistory() {
        if (currentAuction == null) return;

        AuctionDAO auctionDAO = new AuctionDAOImpl();
        List<BidTransaction> historyList = auctionDAO.getBidHistory(currentAuction.getId());

        if (historyList != null) {
            currentBidCount = historyList.size(); // Lưu lại số lượng
            
            ObservableList<BidTransaction> observableList = FXCollections.observableArrayList(historyList);
            tableBids.setItems(observableList);
            
            if (!historyList.isEmpty()) {
                double realHighest = historyList.get(0).getAmount();
                currentAuction.getItem().setCurHighest(realHighest);
                lblCurrentPrice.setText(String.format("%,.0f VNĐ", realHighest));
            }
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
    
    private void startPollingData() {
        if (pollingTimeline != null) pollingTimeline.stop();

        pollingTimeline = new Timeline(new KeyFrame(Duration.seconds(3), event -> {
            if (currentAuction != null && currentAuction.getStatus() == StatusOfAuction.RUNNING) {
                // 1. DÙNG COMPLETABLE FUTURE ĐỂ TẠO LUỒNG CHẠY NGẦM KHÔNG GÂY LAG UI
                CompletableFuture.supplyAsync(() -> {
                    AuctionDAO auctionDAO = new AuctionDAOImpl();
                    // Gọi DB ngầm, giao diện lúc này vẫn gõ phím mượt mà bình thường
                    return auctionDAO.getBidHistory(currentAuction.getId());
                }).thenAccept(historyList -> {
                    // 2. KHI DB TRẢ KẾT QUẢ VỀ -> ĐẨY VÀO LUỒNG UI ĐỂ CẬP NHẬT
                    Platform.runLater(() -> {
                        if (historyList != null && historyList.size() != currentBidCount) {
                            currentBidCount = historyList.size();
                            // Cập nhật Bảng lịch sử
                            ObservableList<BidTransaction> observableList = FXCollections.observableArrayList(historyList);
                            tableBids.setItems(observableList);
                            // Cập nhật giá mới nhất lên Label
                            double realHighest = historyList.get(0).getAmount();
                            currentAuction.getItem().setCurHighest(realHighest);
                            lblCurrentPrice.setText(String.format("%,.0f VNĐ", realHighest));
                        }
                    });
                });
            }
        }));
        pollingTimeline.setCycleCount(Animation.INDEFINITE);
        pollingTimeline.play();
    }
}
