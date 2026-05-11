package com.nhom3.client.controller;

import java.util.List;

import com.nhom3.shared.model.auction.Auction;
import com.nhom3.shared.model.item.Item;
import com.nhom3.server.dao.AuctionDAO;
import com.nhom3.server.dao.AuctionDAOImpl;

import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;

public class MarketController {

    // Liên kết các thành phần từ FXML
    @FXML private FlowPane flowMarket;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cbCategory;
    @FXML private Button btnReload;

    // Lưu trữ danh sách gốc để Lọc/Tìm kiếm không cần gọi lại Database
    private List<Auction> allActiveAuctions;

    @FXML
    public void initialize() {
        System.out.println("Đang khởi tạo Chợ Đấu Giá...");
        
        // 1. Khởi tạo dữ liệu cho ComboBox
        cbCategory.setItems(FXCollections.observableArrayList("Tất cả", "ART", "ELECTRONICS", "VEHICLE"));
        cbCategory.setValue("Tất cả"); // Giá trị mặc định

        // 2. Bắt sự kiện khi bấm nút Làm mới
        btnReload.setOnAction(e -> {
            txtSearch.clear();
            cbCategory.setValue("Tất cả");
            loadMarket();
        });

        // 3. Bắt sự kiện Tìm kiếm (Gõ đến đâu lọc đến đó)
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> filterMarket());

        // 4. Bắt sự kiện Lọc theo loại
        cbCategory.valueProperty().addListener((observable, oldValue, newValue) -> filterMarket());

        // 5. Tải dữ liệu lần đầu tiên
        loadMarket(); 
    }

    @FXML
    public void loadMarket() {
        // Lấy danh sách các phiên đấu giá đang hoạt động (OPEN hoặc RUNNING) từ DB
        AuctionDAO auctionDAO = new AuctionDAOImpl();
        allActiveAuctions = auctionDAO.getActiveAuctions();
        
        // Gọi hàm hiển thị (Hàm này đã bao gồm logic render giao diện)
        filterMarket();
    }

    // Hàm xử lý Lọc và Render Card ra màn hình
    private void filterMarket() {
        // Xóa sạch các thẻ cũ trên giao diện
        flowMarket.getChildren().clear(); 

        if (allActiveAuctions == null || allActiveAuctions.isEmpty()) {
            showEmptyMessage("Hiện tại chưa có sản phẩm nào đang lên sàn.");
            return;
        }

        String searchText = txtSearch.getText().toLowerCase();
        String selectedCategory = cbCategory.getValue();
        int displayCount = 0;

        for (Auction auction : allActiveAuctions) { 
            Item item = auction.getItem();
            
            // Điều kiện lọc theo Loại
            boolean matchCategory = selectedCategory.equals("Tất cả") || item.getType().name().equalsIgnoreCase(selectedCategory);
            
            // Điều kiện tìm kiếm theo Tên
            boolean matchSearch = searchText.isEmpty() || item.getName().toLowerCase().contains(searchText);

            if (matchCategory && matchSearch) {
                VBox card = createProductCard(auction);
                flowMarket.getChildren().add(card);
                displayCount++;
            }
        }

        if (displayCount == 0) {
            showEmptyMessage("Không tìm thấy sản phẩm nào phù hợp với tìm kiếm của bạn.");
        }
    }

    private void showEmptyMessage(String message) {
        Label lblEmpty = new Label(message);
        lblEmpty.setStyle("-fx-text-fill: #7f8c8d; -fx-font-style: italic; -fx-font-size: 16px;");
        flowMarket.getChildren().add(lblEmpty);
    }

    private VBox createProductCard(Auction auction) {
        Item item = auction.getItem();

        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 4);");
        card.setPadding(new Insets(15));
        card.setPrefWidth(220);

        // Ảnh giả lập
        Rectangle imgPlaceholder = new Rectangle(190, 140);
        imgPlaceholder.setStyle("-fx-fill: #ecf0f1; -fx-arc-width: 10; -fx-arc-height: 10;");

        // Lấy tên từ Item
        Label lblName = new Label(item.getName());
        lblName.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        // Lấy phân loại từ Item
        Label lblType = new Label(item.getType().name());
        lblType.setStyle("-fx-text-fill: #7f8c8d;");

        // Lấy giá hiện tại từ Item (curHighest)
        Label lblPrice = new Label(String.format("%,.0f VNĐ", item.getCurHighest()));
        lblPrice.setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-text-fill: #e74c3c;");

        // Nút bấm
        Button btnBid = new Button("Vào Đấu Giá");
        btnBid.setMaxWidth(Double.MAX_VALUE);
        btnBid.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");

        // Khi bấm nút: Mở màn hình chi tiết và truyền dữ liệu thật sang
        btnBid.setOnAction(e -> openItemDetail(item, auction, this::loadMarket));

        card.getChildren().addAll(imgPlaceholder, lblName, lblType, lblPrice, btnBid);
        return card;
    }

    // Hàm mở Modal Chi tiết
    public void openItemDetail(Item item, Auction auction, Runnable onWindowClosed) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/nhom3/client/view/view_item_detail.fxml"));
            Parent root = loader.load();
            
            ViewItemDetailController detailController = loader.getController();
            String statusStr = auction.getStatus().name(); 
            detailController.setItemData(item, auction, statusStr);
            
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Chi tiết sản phẩm - " + item.getName());
            stage.setScene(new Scene(root));
            
            stage.showAndWait();
            
            if (onWindowClosed != null) {
                onWindowClosed.run();
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}