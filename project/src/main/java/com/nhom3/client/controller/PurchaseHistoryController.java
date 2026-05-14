package com.nhom3.client.controller;

import com.nhom3.client.utils.UserSession;
import com.nhom3.shared.model.auction.Auction;
import com.nhom3.shared.model.item.Item;
import com.nhom3.shared.model.auction.BidTransaction;
import com.nhom3.shared.model.user.Bidder;
import com.nhom3.shared.model.auction.StatusOfAuction;
import com.nhom3.shared.model.user.User;
import com.nhom3.client.network.ServerConnection;
import com.nhom3.shared.network.packet.Packet;
import com.nhom3.shared.network.packet.PacketType;
import com.nhom3.shared.network.payload.BidderIdPayload;
import com.nhom3.shared.network.payload.PurchaseHistoryResponsePayload;

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
import java.util.ArrayList;
import java.util.List;

@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
        value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD",
        justification = "JavaFX controllers are reached from the socket dispatcher through the active screen instance.")
public class PurchaseHistoryController {

    @FXML private TableView<Auction> tableHistory;
    @FXML private TableColumn<Auction, String> colItemName;
    @FXML private TableColumn<Auction, String> colBidAmount;
    @FXML private TableColumn<Auction, String> colBidTime;
    @FXML private TableColumn<Auction, String> colAuctionStatus;
    @FXML private TableColumn<Auction, String> colResult;

    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cbFilter;

    private ObservableList<Auction> masterDataList = FXCollections.observableArrayList();

    // ---- THÊM SINGLETON CHO SERVER HANDLER GỌI VỀ ----
    private static PurchaseHistoryController instance;
    public static PurchaseHistoryController getInstance() { return instance; }

    @FXML
    public void initialize() {
        instance = this; // Gán instance
        tableHistory.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        cbFilter.setItems(FXCollections.observableArrayList(
            "Tất cả", "Đang Dẫn Đầu", "Bị Vượt Giá", "CHIẾN THẮNG", "THUA CUỘC"
        ));
        cbFilter.setValue("Tất cả");

        colItemName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getItem().getName()));

        colBidAmount.setCellValueFactory(data -> {
            if(data.getValue().getBidHistory().isEmpty()) return new SimpleStringProperty("0 VNĐ");
            double amount = data.getValue().getBidHistory().get(0).getAmount();
            return new SimpleStringProperty(String.format("%,.0f VNĐ", amount));
        });
        
        colBidTime.setCellValueFactory(data -> {
            if(data.getValue().getBidHistory().isEmpty()) return new SimpleStringProperty("");
            LocalDateTime time = data.getValue().getBidHistory().get(0).getBidTime();
            return new SimpleStringProperty(time.format(DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy")));
        });

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

        colResult.setCellValueFactory(data -> new SimpleStringProperty(getAuctionResultStatus(data.getValue())));

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
                        if (item.contains("Đang Dẫn Đầu")) btnAction.setStyle(baseStyle + "-fx-background-color: #27ae60;"); 
                        else if (item.contains("Bị Vượt Giá")) btnAction.setStyle(baseStyle + "-fx-background-color: #e67e22;"); 
                        else if (item.contains("CHIẾN THẮNG")) btnAction.setStyle(baseStyle + "-fx-background-color: #f1c40f; -fx-text-fill: #2c3e50;"); 
                        else btnAction.setStyle(baseStyle + "-fx-background-color: #95a5a6;"); 
                        
                        setGraphic(btnAction);
                        setAlignment(javafx.geometry.Pos.CENTER);
                    }
                }
            };
        });

        setupSearchAndFilter();
        loadHistoryData(); // Gọi để load dữ liệu qua mạng
    }

    private void setupSearchAndFilter() {
        FilteredList<Auction> filteredData = new FilteredList<>(masterDataList, b -> true);

        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(auction -> checkFilter(auction, newValue, cbFilter.getValue()));
        });

        cbFilter.valueProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(auction -> checkFilter(auction, txtSearch.getText(), newValue));
        });

        SortedList<Auction> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableHistory.comparatorProperty());
        tableHistory.setItems(sortedData);
    }

    private boolean checkFilter(Auction auction, String searchText, String filterStatus) {
        boolean matchesFilter = true;
        if (filterStatus != null && !filterStatus.equals("Tất cả")) {
            matchesFilter = getAuctionResultStatus(auction).equals(filterStatus);
        }
        boolean matchesSearch = true;
        if (searchText != null && !searchText.isEmpty()) {
            matchesSearch = auction.getItem().getName().toLowerCase().contains(searchText.toLowerCase());
        }
        return matchesFilter && matchesSearch;
    }

    private String getAuctionResultStatus(Auction a) {
        User me = UserSession.getInstance().getLoggedInUser();
        int top1Id = -1;
        try { top1Id = a.getHighestBidder().getId(); } catch (Exception e) {}

        boolean isMeTop1 = (top1Id == me.getId());
        String status = a.getStatus().name();
        
        if (status.equals("RUNNING") || status.equals("OPEN")) return isMeTop1 ? "Đang Dẫn Đầu" : "Bị Vượt Giá";
        else if (status.equals("FINISHED") || status.equals("PAID")) return isMeTop1 ? "CHIẾN THẮNG" : "THUA CUỘC";
        return "-";
    }

    // ==== GỬI YÊU CẦU LÊN SERVER ====
    public void loadHistoryData() {
        User currentUser = UserSession.getInstance().getLoggedInUser();
        if (currentUser == null) return;

        try {
            BidderIdPayload payload = new BidderIdPayload(currentUser.getId());
            Packet packet = new Packet(PacketType.LOAD_PURCHASE_HISTORY, payload);
            ServerConnection.getInstance().sendMessage(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // ==== NHẬN KẾT QUẢ TỪ SERVER ====
    public void handleLoadHistoryResult(List<PurchaseHistoryResponsePayload.HistoryDTO> dtoList) {
        if (dtoList == null) return;

        List<Auction> realList = new ArrayList<>();
        
        for (PurchaseHistoryResponsePayload.HistoryDTO dto : dtoList) {
            
            // 1. TÁI TẠO ITEM ĐẦY ĐỦ (Dùng Factory để có đúng class)
            Item item = null;
            if ("ART".equals(dto.itemType)) item = new com.nhom3.shared.factory.ArtCreator().createItem(dto.itemId, dto.itemName, dto.startPrice);
            else if ("ELECTRONICS".equals(dto.itemType)) item = new com.nhom3.shared.factory.ElectronicsCreator().createItem(dto.itemId, dto.itemName, dto.startPrice);
            else if ("VEHICLE".equals(dto.itemType)) item = new com.nhom3.shared.factory.VehicleCreator().createItem(dto.itemId, dto.itemName, dto.startPrice);
            else item = new com.nhom3.shared.model.item.Art(dto.itemId, dto.itemName, dto.startPrice); // Fallback
            item.setCurHighest(dto.curHighest);

            // 2. TÁI TẠO THỜI GIAN ĐẦY ĐỦ
            LocalDateTime start = LocalDateTime.now();
            LocalDateTime end = LocalDateTime.now();
            try {
                start = LocalDateTime.parse(dto.startTimeStr);
                end = LocalDateTime.parse(dto.endTimeStr);
            } catch (Exception e) {}

            Auction auction = new Auction(dto.auctionId, item, start, end); 
            
            try { auction.setStatus(StatusOfAuction.valueOf(dto.status)); } 
            catch (Exception e) { auction.setStatus(StatusOfAuction.OPEN); }

            Bidder topBidder = new Bidder(dto.topBidderId, null, null);
            auction.setHighestBidder(topBidder);

            LocalDateTime bidTime = LocalDateTime.now();
            try { bidTime = LocalDateTime.parse(dto.myBidTimeStr); } catch (Exception e) {}
            
            BidTransaction myBid = new BidTransaction(0, null, dto.myBidAmount, bidTime, "");
            auction.getBidHistory().add(myBid);

            realList.add(auction);
        }

        // Ép vẽ lại trên giao diện
        masterDataList.setAll(realList);
        tableHistory.refresh();
    }

    private void navigateToDetail(Auction auction) {
        MarketController marketController = new MarketController();
        marketController.openItemDetail(
            auction.getItem(), 
            auction, 
            () -> loadHistoryData() 
        );
    }
}
