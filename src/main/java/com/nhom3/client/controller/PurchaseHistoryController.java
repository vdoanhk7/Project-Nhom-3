package com.nhom3.client.controller;

import com.nhom3.client.utils.UserSession;
import com.nhom3.server.dao.AuctionDAO;
import com.nhom3.server.dao.AuctionDAOImpl;
import com.nhom3.shared.model.auction.Auction;
import com.nhom3.shared.model.user.User;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TableCell;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PurchaseHistoryController {

    @FXML private TableView<Auction> tableHistory;
    @FXML private TableColumn<Auction, String> colItemName;
    @FXML private TableColumn<Auction, String> colBidAmount;
    @FXML private TableColumn<Auction, String> colBidTime;
    @FXML private TableColumn<Auction, String> colAuctionStatus;
    @FXML private TableColumn<Auction, String> colResult;

    // THÊM 2 BIẾN GIAO DIỆN MỚI
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cbFilter;

    // Danh sách gốc chứa toàn bộ dữ liệu
    private ObservableList<Auction> masterDataList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        tableHistory.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Khởi tạo ComboBox Lọc
        cbFilter.setItems(FXCollections.observableArrayList(
            "Tất cả", "Đang Dẫn Đầu", "Bị Vượt Giá", "CHIẾN THẮNG", "THUA CUỘC"
        ));
        cbFilter.setValue("Tất cả");

        // 1. Lấy Tên sản phẩm
        colItemName.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getItem().getName())
        );

        // 2. Lấy Mức giá bạn đặt
        colBidAmount.setCellValueFactory(data -> {
            double amount = data.getValue().getBidHistory().get(0).getAmount();
            return new SimpleStringProperty(String.format("%,.0f VNĐ", amount));
        });
        
        // 3. Lấy Thời gian đặt
        colBidTime.setCellValueFactory(data -> {
            LocalDateTime time = data.getValue().getBidHistory().get(0).getBidTime();
            return new SimpleStringProperty(time.format(DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy")));
        });

        // 4. Trạng thái phiên đấu giá
        colAuctionStatus.setCellValueFactory(data -> {
            String status = data.getValue().getStatus().name();
            switch (status) {
                case "OPEN": return new SimpleStringProperty("Sắp mở");
                case "RUNNING": return new SimpleStringProperty("Đang diễn ra");
                case "FINISHED": return new SimpleStringProperty("Chờ thanh toán");
                case "PAID": return new SimpleStringProperty("Đã hoàn tất");
                case "CANCELLED": return new SimpleStringProperty("Bị hủy");
                default: return new SimpleStringProperty(status);
            }
        });

        // 5. Kết quả (Gọi hàm dùng chung)
        colResult.setCellValueFactory(data -> 
            new SimpleStringProperty(getAuctionResultStatus(data.getValue()))
        );

        // Tùy chỉnh Nút bấm cho cột Kết quả
        colResult.setCellFactory(column -> {
            return new TableCell<Auction, String>() {
                private final Button btnAction = new Button();

                {
                    btnAction.setPrefWidth(130);
                    btnAction.setPrefHeight(32);
                    btnAction.setCursor(javafx.scene.Cursor.HAND);

                    btnAction.setOnAction(event -> {
                        Auction selectedAuction = getTableView().getItems().get(getIndex());
                        navigateToDetail(selectedAuction);
                    });
                }

                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null || item.equals("-")) {
                        setGraphic(null);
                    } else {
                        btnAction.setText(item);
                        String baseStyle = "-fx-font-weight: bold; -fx-text-fill: white; -fx-background-radius: 15; ";
                        if (item.contains("Đang Dẫn Đầu")) {
                            btnAction.setStyle(baseStyle + "-fx-background-color: #27ae60;"); 
                        } else if (item.contains("Bị Vượt Giá")) {    
                            btnAction.setStyle(baseStyle + "-fx-background-color: #e67e22;"); 
                        } else if (item.contains("CHIẾN THẮNG")) {
                            btnAction.setStyle(baseStyle + "-fx-background-color: #f1c40f; -fx-text-fill: #2c3e50;"); 
                        } else {
                            btnAction.setStyle(baseStyle + "-fx-background-color: #95a5a6;"); 
                        }
                        
                        setGraphic(btnAction);
                        setAlignment(javafx.geometry.Pos.CENTER);
                    }
                }
            };
        });

        // Kích hoạt chức năng tìm kiếm và lọc
        setupSearchAndFilter();

        // Tải dữ liệu từ Database
        loadHistoryData();
    }

    // ==============================================================
    // LOGIC TÌM KIẾM VÀ LỌC
    // ==============================================================
    private void setupSearchAndFilter() {
        FilteredList<Auction> filteredData = new FilteredList<>(masterDataList, b -> true);

        // Bắt sự kiện khi gõ vào ô tìm kiếm
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(auction -> checkFilter(auction, newValue, cbFilter.getValue()));
        });

        // Bắt sự kiện khi chọn ComboBox
        cbFilter.valueProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(auction -> checkFilter(auction, txtSearch.getText(), newValue));
        });

        // Bọc vào SortedList để hỗ trợ click vào tiêu đề cột để sắp xếp
        SortedList<Auction> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableHistory.comparatorProperty());
        tableHistory.setItems(sortedData);
    }

    private boolean checkFilter(Auction auction, String searchText, String filterStatus) {
        // Kiểm tra Lọc theo Trạng thái (Kết quả)
        boolean matchesFilter = true;
        if (filterStatus != null && !filterStatus.equals("Tất cả")) {
            String result = getAuctionResultStatus(auction);
            matchesFilter = result.equals(filterStatus);
        }

        // Kiểm tra Tìm kiếm theo Tên sản phẩm
        boolean matchesSearch = true;
        if (searchText != null && !searchText.isEmpty()) {
            matchesSearch = auction.getItem().getName().toLowerCase().contains(searchText.toLowerCase());
        }

        return matchesFilter && matchesSearch;
    }

    // Hàm dùng chung để tính toán xem người dùng Đang dẫn đầu/Thắng/Thua...
    private String getAuctionResultStatus(Auction a) {
        User me = UserSession.getInstance().getLoggedInUser();
        int top1Id = -1;
        try { top1Id = a.getHighestBidder().getId(); } catch (Exception e) {}

        boolean isMeTop1 = (top1Id == me.getId());
        String status = a.getStatus().name();
        
        if (status.equals("RUNNING") || status.equals("OPEN")) {
            return isMeTop1 ? "Đang Dẫn Đầu" : "Bị Vượt Giá";
        } else if (status.equals("FINISHED") || status.equals("PAID")) {
            return isMeTop1 ? "CHIẾN THẮNG" : "THUA CUỘC";
        }
        return "-";
    }

    // ==============================================================
    // DATA & ĐIỀU HƯỚNG
    // ==============================================================
    private void loadHistoryData() {
        User currentUser = UserSession.getInstance().getLoggedInUser();
        if (currentUser == null) return;

        AuctionDAO auctionDAO = new AuctionDAOImpl();
        List<Auction> historyList = auctionDAO.getMyBidHistory(currentUser.getId());
        
        // Đổ dữ liệu vào list gốc, FilteredList phía trên sẽ tự động cập nhật
        masterDataList.setAll(historyList);
    }

    private void navigateToDetail(Auction auction) {
        MarketController marketController = new MarketController();
        marketController.openItemDetail(
            auction.getItem(), 
            auction, 
            () -> loadHistoryData() // Refresh lại danh sách sau khi đóng popup
        );
    }
}