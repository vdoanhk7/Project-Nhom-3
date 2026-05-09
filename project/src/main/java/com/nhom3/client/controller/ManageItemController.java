package com.nhom3.client.controller;

import com.nhom3.client.utils.UserSession;
import com.nhom3.server.dao.AuctionDAO;
import com.nhom3.server.dao.AuctionDAOImpl;
import com.nhom3.server.dao.ItemDAO;
import com.nhom3.server.dao.ItemDAOImpl;
import com.nhom3.shared.model.auction.Auction;
import com.nhom3.shared.model.item.Item;
import com.nhom3.shared.model.user.Seller;
import com.nhom3.shared.model.user.User;
import com.nhom3.client.network.ServerConnection;
import com.nhom3.shared.network.packet.Packet;
import com.nhom3.shared.network.packet.PacketType;
import com.nhom3.shared.network.payload.SellerIdPayload;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageItemController {

    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cbCategory;
    @FXML private TableView<Item> tableItems;
    @FXML private TableColumn<Item, Integer> colId;
    @FXML private TableColumn<Item, String> colName;
    @FXML private TableColumn<Item, String> colType;
    @FXML private TableColumn<Item, Double> colStartPrice;
    @FXML private TableColumn<Item, Double> colCurHighest;
    @FXML private TableColumn<Item, Void> colAction;

    private ObservableList<Item> itemList = FXCollections.observableArrayList();
    private Map<Integer, String> itemStatusMap = new HashMap<>();
    private static ManageItemController instance;
    public static ManageItemController getInstance() {
        return instance;
    }
    @FXML
    public void initialize() {
        instance = this;
        // 1. Khởi tạo dữ liệu cho ComboBox Lọc
        cbCategory.setItems(FXCollections.observableArrayList("Tất cả", "ART", "ELECTRONICS", "VEHICLE"));

        // 2. Cài đặt các cột cho TableView bám sát thuộc tính của class Item
        // Lấy giá trị ID từ class Item
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setCellFactory(tc -> new TableCell<Item, Integer>() {
            @Override
            protected void updateItem(Integer id, boolean empty) {
                super.updateItem(id, empty);
                if (empty || id == null) {
                    setText(null);
                } else {
                    setText(String.format("SP-%04d", id));
                }
            }
        });
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        
        // Format hiển thị tiền tệ cho Giá khởi điểm
        colStartPrice.setCellValueFactory(new PropertyValueFactory<>("startPrice"));
        colStartPrice.setCellFactory(tc -> new TableCell<Item, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(String.format("%,.0f VNĐ", price));
                }
            }
        });

        // Format hiển thị tiền tệ cho Giá hiện tại
        colCurHighest.setCellValueFactory(new PropertyValueFactory<>("curHighest"));
        colCurHighest.setCellFactory(colStartPrice.getCellFactory());

        // Cột Thao tác (Tạo nút Edit/Delete trực tiếp)
        setupActionColumn();

        // Thiết lập chức năng tìm kiếm và lọc
        setupSearchAndFilter();

        // 3. Load dữ liệu Item của Seller hiện tại vào bảng
        loadSellerItems();
    }

    // Hàm tìm kiếm dữ liệu item
    private void setupSearchAndFilter() {
        FilteredList<Item> filteredData = new FilteredList<>(itemList, b -> true);
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(item -> checkFilter(item, newValue, cbCategory.getValue()));
        });
        cbCategory.valueProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(item -> checkFilter(item, txtSearch.getText(), newValue));
        });
        SortedList<Item> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableItems.comparatorProperty());
        tableItems.setItems(sortedData);
    }

    // Hàm hỗ trợ kiểm tra điều kiện lọc
    private boolean checkFilter(Item item, String searchText, String category) {
        // Kiểm tra Phân loại 
        boolean matchesCategory = true;
        if (category != null && !category.equals("Tất cả")) {
            // So sánh loại của item với giá trị trong ComboBox
            matchesCategory = item.getType().name().equalsIgnoreCase(category);
        }
        // Kiểm tra Tìm kiếm 
        boolean matchesSearch = true;
        if (searchText != null && !searchText.isEmpty()) {
            String lowerCaseFilter = searchText.toLowerCase();
            // Tạo chuỗi mã SP để tìm (Ví dụ: "sp-0001")
            String itemCode = String.format("SP-%04d", item.getId()).toLowerCase();
            // Điều kiện: Tên sản phẩm chứa từ khóa HOẶC Mã SP chứa từ khóa
            matchesSearch = item.getName().toLowerCase().contains(lowerCaseFilter) || 
                            itemCode.contains(lowerCaseFilter);
        }
        // Item chỉ được hiển thị nếu thỏa mãn CẢ 2 điều kiện
        return matchesCategory && matchesSearch;
    }

    public void loadSellerItems() { // Đổi thành public để các Pop-up gọi lại được
        User currentUser = UserSession.getInstance().getLoggedInUser();
        if (currentUser instanceof Seller) {
            // GÓI HÀNG GỬI QUA MẠNG
            SellerIdPayload payload = new SellerIdPayload(currentUser.getId());
            Packet packet = new Packet(PacketType.LOAD_SELLER_ITEMS, payload);
            
            try {
                ServerConnection.getInstance().sendMessage(packet);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void setupActionColumn() {
        colAction.setCellFactory(param -> new TableCell<>() {
        // Khai báo các nút bấm
        private final Button btnPublish = new Button("Đăng bán");
        private final Button btnEdit = new Button("Sửa");
        private final Button btnDelete = new Button("Xóa");
        private final Button btnView = new Button("Xem Chi Tiết");
        private final Button btnConfirmPaid = new Button("Xác Nhận");
        private final Label lblPaidStatus = new Label("✔ Bán Thành Công");
        private final HBox pane = new HBox(8, btnPublish, btnEdit, btnDelete, btnView, btnConfirmPaid, lblPaidStatus);
        {
            // Styling cơ bản
            btnPublish.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-cursor: hand;");
            btnEdit.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-cursor: hand;");
            btnDelete.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand;");
            btnView.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-cursor: hand;");
            pane.setStyle("-fx-alignment: center;");
            btnConfirmPaid.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
            lblPaidStatus.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 5 10 5 0;");
            
            // Các sự kiện OnAction cho từng nút
            btnPublish.setOnAction(e -> handlePublish(getTableView().getItems().get(getIndex())));
            btnEdit.setOnAction(e -> handleEdit(getTableView().getItems().get(getIndex())));
            btnDelete.setOnAction(e -> handleDelete(getTableView().getItems().get(getIndex())));
            btnView.setOnAction(e -> handleViewDetail(getTableView().getItems().get(getIndex())));
            btnConfirmPaid.setOnAction(e -> handleConfirmPaid(getTableView().getItems().get(getIndex())));
        }

        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || getIndex() >= getTableView().getItems().size()) {
                setGraphic(null);
            } else {
                Item currentItem = getTableView().getItems().get(getIndex());
                boolean hasAuction = checkHasAuction(currentItem); 
                String status = getAuctionStatus(currentItem);

                // 1. ẨN TẤT CẢ ĐI TRƯỚC (Bắt buộc phải làm sạch dòng trước khi đắp nút mới)
                // Lưu ý: Nhớ khai báo biến lblPaidStatus ở phần đầu của setupActionColumn nhé!
                lblPaidStatus.setVisible(false);  lblPaidStatus.setManaged(false);
                btnPublish.setVisible(false);     btnPublish.setManaged(false);
                btnEdit.setVisible(false);        btnEdit.setManaged(false);
                btnDelete.setVisible(false);      btnDelete.setManaged(false);
                btnConfirmPaid.setVisible(false); btnConfirmPaid.setManaged(false);
                btnView.setVisible(false);        btnView.setManaged(false);

                // 2. LOGIC HIỂN THỊ (Sử dụng lại hàm showButtons của bạn)
                if (status.equals("PAID")) {
                    // Khi đã thanh toán: Hiện nhãn báo thành công
                    lblPaidStatus.setVisible(true); lblPaidStatus.setManaged(true);
                    showButtons(btnView);
                    
                } else if (status.equals("FINISHED")) {
                    // Khi kết thúc: Hiện nút Xác Nhận
                    btnConfirmPaid.setVisible(true); btnConfirmPaid.setManaged(true);
                    showButtons(btnView);
                    
                } else if (!hasAuction || status.equals("CANCELLED")) {
                    // Chưa lên sàn hoặc bị Hủy: Hiện bộ 3 nút Edit
                    showButtons(btnPublish, btnEdit, btnDelete);
                    
                } else {
                    // Đã lên sàn và Đang chạy (OPEN, RUNNING)
                    showButtons(btnView);
                }

                // 3. GIỮ NGUYÊN PHẦN STYLE CŨ CỦA BẠN CHO NÚT XEM
                btnView.setText("Xem Chi Tiết"); 
                btnView.setStyle(
                    "-fx-background-color: #3498db; " +
                    "-fx-text-fill: white; " +
                    "-fx-font-weight: bold; " +
                    "-fx-background-radius: 5; " +
                    "-fx-padding: 5 15 5 15; " +
                    "-fx-cursor: hand;"
                );

                setGraphic(pane);
            }
        }

        // Hàm hỗ trợ bật hiển thị nút
        private void showButtons(Button... buttons) {
            for (Button b : buttons) {
                b.setVisible(true);
                b.setManaged(true);
            }
        }
    });
}

    @FXML
    void handleAddNewItem(ActionEvent event) {
        try {
            // 1. Tải file giao diện add_item.fxml
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/nhom3/client/view/add_item.fxml"));
            Parent root = loader.load();
            // 2. Tạo một cửa sổ mới cho Pop-up
            Stage popupStage = new Stage();
            popupStage.setTitle("Thêm Sản Phẩm Mới");
            // 3. Thiết lập chế độ MODAL (Buộc người dùng xong tác cụ mới đc thoát ra)
            popupStage.initModality(Modality.APPLICATION_MODAL);
            // 4. Hiển thị cửa sổ
            popupStage.setScene(new Scene(root));
            popupStage.setResizable(false);
            // 5. Dừng luồng code tại đây và CHỜ cho đến khi cửa sổ Pop-up đóng lại
            popupStage.showAndWait(); 
            // 6. CẬP NHẬT LẠI BẢNG DỮ LIỆU (Auto Refresh)
            loadSellerItems();
            tableItems.refresh();

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Lỗi: Không tìm thấy file add_item.fxml tại đường dẫn đã chỉ định.");
            
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi hệ thống");
            alert.setHeaderText(null);
            alert.setContentText("Không thể mở cửa sổ thêm sản phẩm. Vui lòng thử lại sau!");
            alert.showAndWait();
        }
    }


    // CÁC HÀM XỬ LÝ SỰ KIỆN KHI BẤM NÚT TRONG BẢNG
    private void handlePublish(Item item) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/nhom3/client/view/publish_auction.fxml"));
            Parent root = loader.load();

            PublishAuctionController controller = loader.getController();
            controller.setItem(item); // Truyền món đồ sang Pop-up

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Đăng bán sản phẩm");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
            
            loadSellerItems(); // Tải lại bảng để cập nhật trạng thái các nút bấm
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleEdit(Item item) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/nhom3/client/view/add_item.fxml"));
            Parent root = loader.load();

            // 👉 LẤY CONTROLLER VÀ TRUYỀN DỮ LIỆU SANG
            AddItemController controller = loader.getController();
            controller.setEditingItem(item); 

            Stage popupStage = new Stage();
            popupStage.setTitle("Sửa Thông Tin Sản Phẩm: " + item.getName());
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setScene(new Scene(root));
            popupStage.setResizable(false);           
            popupStage.showAndWait(); // Dừng chờ người dùng sửa xong  
            tableItems.refresh(); // Tự động vẽ lại bảng với dữ liệu mới trong RAM
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Lỗi mở Pop-up sửa sản phẩm!");
        }
    }

    private void handleDelete(Item item) {
        // Lấy thông tin người dùng đang đăng nhập
        User currentUser = UserSession.getInstance().getLoggedInUser();
        // Kiểm tra chắc chắn người dùng là Seller
        if (!(currentUser instanceof Seller)) {
             Alert alert = new Alert(Alert.AlertType.ERROR, "Bạn không có quyền thực hiện thao tác này!");
             alert.showAndWait();
             return;
        }
        int sellerId = currentUser.getId();
        // 1. Hiển thị hộp thoại xác nhận 
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận xóa");
        alert.setHeaderText(null);
        alert.setContentText("Bạn có chắc chắn muốn xóa vĩnh viễn sản phẩm: " + item.getName() + "?");
        
        // 2. Chờ người dùng bấm nút
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            // 3. Gọi DAO để xóa dưới Database, truyền cả itemId và sellerId
            ItemDAO itemDAO = new ItemDAOImpl();
            boolean isSuccess = itemDAO.deleteItem(item.getId(), sellerId); 
            if (isSuccess) {
                // 4. Xóa ngay trên giao diện 
                itemList.remove(item);
                System.out.println("Đã xóa thành công: " + item.getName());
            } else {
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Lỗi");
                errorAlert.setHeaderText(null);
                errorAlert.setContentText("Không thể xóa sản phẩm. Vui lòng thử lại sau!");
                errorAlert.showAndWait();
            }
        }
    }

    private void handleViewDetail(Item item) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/nhom3/client/view/view_item_detail.fxml"));
            Parent root = loader.load();
            
            ViewItemDetailController controller = loader.getController();
            // 1. Lấy status từ Map
            String status = getAuctionStatus(item);
            // 2. THÊM 2 DÒNG NÀY: Lấy Auction từ Database
            AuctionDAO auctionDAO = new AuctionDAOImpl();
            Auction auction = auctionDAO.getAuctionByItemId(item.getId());
            if (auction != null) {
                auction.setItem(item); 
            }
            // 3. SỬA DÒNG BÁO LỖI THÀNH DÒNG NÀY: Truyền đủ 3 tham số
            controller.setItemData(item, auction, status);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Chi tiết đấu giá: " + item.getName());
            stage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void handleConfirmPaid(Item item) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, 
            "Bạn xác nhận đã nhận đủ tiền cho sản phẩm: " + item.getName() + "?\nLưu ý: Sau khi xác nhận không thể hoàn tác!", 
            ButtonType.YES, ButtonType.NO);
        if (alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            AuctionDAO auctionDAO = new AuctionDAOImpl();
            // 1. Tìm ID của phiên đấu giá thông qua ID của sản phẩm
            com.nhom3.shared.model.auction.Auction auction = auctionDAO.getAuctionByItemId(item.getId());
            if (auction != null) {
                // 2. Gọi Database để cập nhật trạng thái
                boolean isSuccess = auctionDAO.confirmPayment(auction.getId());
                if (isSuccess) {
                    System.out.println("Đã xác nhận thanh toán cho: " + item.getName());
                    // 3. Tải lại bảng 
                    loadSellerItems(); 
                } else {
                    new Alert(Alert.AlertType.ERROR, "Có lỗi xảy ra khi cập nhật Database!").showAndWait();
                }
            }
        }
    }
    public void handleLoadItemsResult(List<com.nhom3.shared.network.payload.SellerItemsResponsePayload.SellerItemDTO> dtoList) {
        if (dtoList == null) return;

        List<Item> realItems = new java.util.ArrayList<>();
        itemStatusMap.clear();

        for (com.nhom3.shared.network.payload.SellerItemsResponsePayload.SellerItemDTO dto : dtoList) {
            // 1. Tái tạo lại Object Item tùy theo Type
            Item item = null;
            com.nhom3.shared.model.item.ItemType typeEnum = com.nhom3.shared.model.item.ItemType.valueOf(dto.type);
            item = typeEnum.createItem(dto.id, dto.name, dto.startPrice);
            if (item != null) {
                item.setCurHighest(dto.curHighest);
                realItems.add(item);
            }

            // 2. Nạp lại Map trạng thái
            if (dto.status != null && !dto.status.isEmpty()) {
                itemStatusMap.put(dto.id, dto.status);
            }
        }

        // Cập nhật giao diện
        itemList.setAll(realItems);
        tableItems.refresh();
    }


    private boolean checkHasAuction(Item item) {
        // Nếu ID của item này có trong Map, nghĩa là nó đã được đăng bán
        return !getAuctionStatus(item).isEmpty();
    }

    private String getAuctionStatus(Item item) {
        // Lấy trạng thái từ Map, nếu không có thì trả về chuỗi rỗng
        return itemStatusMap.getOrDefault(item.getId(), "");
    }
}