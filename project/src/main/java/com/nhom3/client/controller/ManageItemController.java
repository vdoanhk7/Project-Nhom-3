package com.nhom3.client.controller;

import com.nhom3.client.network.ServerConnection;
import com.nhom3.client.utils.UserSession;
import com.nhom3.shared.model.auction.Auction;
import com.nhom3.shared.model.auction.StatusOfAuction;
import com.nhom3.shared.model.item.Item;
import com.nhom3.shared.model.item.ItemType;
import com.nhom3.shared.model.user.Bidder;
import com.nhom3.shared.model.user.Seller;
import com.nhom3.shared.model.user.User;
import com.nhom3.shared.network.packet.Packet;
import com.nhom3.shared.network.packet.PacketType;
import com.nhom3.shared.network.payload.AuctionListResponsePayload;
import com.nhom3.shared.network.payload.ItemActionPayload;
import com.nhom3.shared.network.payload.ItemImagePayload;
import com.nhom3.shared.network.payload.SellerIdPayload;
import com.nhom3.shared.network.payload.SellerItemsResponsePayload;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
        value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD",
        justification = "JavaFX controllers are reached from the socket dispatcher through the active screen instance.")
public class ManageItemController {

    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cbCategory;
    @FXML private TableView<Item> tableItems;
    @FXML private TableColumn<Item, Integer> colId;
    @FXML private TableColumn<Item, Void> colImage;
    @FXML private TableColumn<Item, String> colName;
    @FXML private TableColumn<Item, String> colType;
    @FXML private TableColumn<Item, Double> colStartPrice;
    @FXML private TableColumn<Item, Double> colCurHighest;
    @FXML private TableColumn<Item, Void> colAction;

    private static final ExecutorService IMAGE_LOADER = Executors.newFixedThreadPool(4, runnable -> {
        Thread thread = new Thread(runnable, "ManageItem-ImageLoader");
        thread.setDaemon(true);
        return thread;
    });

    private final ObservableList<Item> itemList = FXCollections.observableArrayList();
    private final Map<Integer, String> itemStatusMap = new HashMap<>();
    private final Map<Integer, String> imageCache = new ConcurrentHashMap<>();
    private final Set<Integer> requestedImageIds = ConcurrentHashMap.newKeySet();
    private final Set<Integer> noImageIds = ConcurrentHashMap.newKeySet();
    private Item pendingDeleteItem;
    private Item pendingViewItem;

    private static ManageItemController instance;

    public static ManageItemController getInstance() {
        return instance;
    }

    @FXML
    public void initialize() {
        instance = this;
        cbCategory.setItems(FXCollections.observableArrayList("Tất cả", "ART", "ELECTRONICS", "VEHICLE"));
        setupTableColumns();
        setupActionColumn();
        setupSearchAndFilter();
        loadSellerItems();
    }

    public void loadSellerItems() {
        User currentUser = UserSession.getInstance().getLoggedInUser();
        if (currentUser instanceof Seller) {
            try {
                SellerIdPayload payload = new SellerIdPayload(currentUser.getId());
                ServerConnection.getInstance().sendMessage(new Packet(PacketType.LOAD_SELLER_ITEMS, payload));
            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Lỗi mạng", "Không thể tải danh sách sản phẩm!");
            }
        }
    }

    public void handleLoadItemsResult(List<SellerItemsResponsePayload.SellerItemDTO> dtoList) {
        if (dtoList == null) {
            return;
        }

        List<Item> realItems = new java.util.ArrayList<>();
        itemStatusMap.clear();
        requestedImageIds.clear();
        imageCache.clear();
        noImageIds.clear();
        for (SellerItemsResponsePayload.SellerItemDTO dto : dtoList) {
            ItemType type = ItemType.valueOf(dto.type);
            Item item = type.createItem(dto.id, dto.name, dto.startPrice);
            item.setCurHighest(dto.curHighest);
            realItems.add(item);
            if (dto.status != null && !dto.status.isEmpty()) {
                itemStatusMap.put(dto.id, dto.status);
            }
        }
        itemList.setAll(realItems);
        tableItems.refresh();
    }

    public void handleItemImageResult(ItemImagePayload payload) {
        if (payload == null || payload.getItemId() <= 0) {
            return;
        }

        String imageBase64 = payload.getImageBase64();
        if (imageBase64 == null || imageBase64.isEmpty()) {
            noImageIds.add(payload.getItemId());
            tableItems.refresh();
            return;
        }

        imageCache.put(payload.getItemId(), imageBase64);
        for (Item item : itemList) {
            if (item.getId() == payload.getItemId()) {
                item.setImageBase64(imageBase64);
                break;
            }
        }
        if (pendingViewItem != null && pendingViewItem.getId() == payload.getItemId()) {
            pendingViewItem.setImageBase64(imageBase64);
        }
        tableItems.refresh();
    }

    public void handleDeleteItemResult(boolean success, String message) {
        showAlert(success ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR,
                success ? "Thành công" : "Thất bại", message);
        if (success && pendingDeleteItem != null) {
            itemList.remove(pendingDeleteItem);
        }
        pendingDeleteItem = null;
    }

    public void handleConfirmPaymentResult(boolean success, String message) {
        showAlert(success ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR,
                success ? "Thành công" : "Thất bại", message);
        if (success) {
            loadSellerItems();
        }
    }

    public void handleLoadAuctionByItemResult(List<AuctionListResponsePayload.AuctionDTO> dtoList) {
        if (pendingViewItem == null) {
            return;
        }

        Auction auction = null;
        if (dtoList != null && !dtoList.isEmpty()) {
            auction = toAuction(dtoList.get(0));
        }
        openDetailWindow(pendingViewItem, auction, getAuctionStatus(pendingViewItem));
        pendingViewItem = null;
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Integer id, boolean empty) {
                super.updateItem(id, empty);
                setText(empty || id == null ? null : String.format("SP-%04d", id));
            }
        });
        
        colImage.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }
                Item currentItem = getTableView().getItems().get(getIndex());
                if (currentItem.getImageBase64() != null && !currentItem.getImageBase64().isEmpty()) {
                    try {
                        byte[] imageBytes = java.util.Base64.getDecoder().decode(currentItem.getImageBase64());
                        javafx.scene.image.Image img = new javafx.scene.image.Image(new java.io.ByteArrayInputStream(imageBytes));
                        javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView(img);
                        imageView.setFitWidth(60);
                        imageView.setFitHeight(60);
                        imageView.setPreserveRatio(true);
                        setGraphic(imageView);
                        setAlignment(javafx.geometry.Pos.CENTER);
                    } catch (Exception e) {
                        setGraphic(null);
                    }
                } else {
                    setGraphic(createImagePlaceholder(
                            noImageIds.contains(currentItem.getId()) ? "No Image" : "..."));
                    setAlignment(Pos.CENTER);
                    requestItemImage(currentItem.getId());
                }
            }
        });

        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colStartPrice.setCellValueFactory(new PropertyValueFactory<>("startPrice"));
        colCurHighest.setCellValueFactory(new PropertyValueFactory<>("curHighest"));
        colStartPrice.setCellFactory(tc -> moneyCell());
        colCurHighest.setCellFactory(tc -> moneyCell());
    }

    private TableCell<Item, Double> moneyCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                setText(empty || price == null ? null : String.format("%,.0f VND", price));
            }
        };
    }

    private StackPane createImagePlaceholder(String text) {
        StackPane placeholder = new StackPane();
        placeholder.setPrefSize(60, 60);
        placeholder.setMaxSize(60, 60);
        placeholder.setStyle("-fx-background-color: #e5e7eb; -fx-background-radius: 6;");
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #6b7280; -fx-font-weight: bold; -fx-font-size: 10px;");
        placeholder.getChildren().add(label);
        return placeholder;
    }

    private void requestItemImage(int itemId) {
        if (itemId <= 0 || imageCache.containsKey(itemId) || noImageIds.contains(itemId)
                || !requestedImageIds.add(itemId)) {
            return;
        }

        IMAGE_LOADER.submit(() -> {
            try {
                ServerConnection.getInstance().sendMessage(
                        new Packet(PacketType.LOAD_ITEM_IMAGE, new ItemActionPayload(itemId, 0)));
            } catch (Exception e) {
                requestedImageIds.remove(itemId);
                e.printStackTrace();
            }
        });
    }

    private void setupSearchAndFilter() {
        FilteredList<Item> filteredData = new FilteredList<>(itemList, item -> true);
        txtSearch.textProperty().addListener((observable, oldValue, newValue) ->
                filteredData.setPredicate(item -> checkFilter(item, newValue, cbCategory.getValue())));
        cbCategory.valueProperty().addListener((observable, oldValue, newValue) ->
                filteredData.setPredicate(item -> checkFilter(item, txtSearch.getText(), newValue)));
        SortedList<Item> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableItems.comparatorProperty());
        tableItems.setItems(sortedData);
    }

    private boolean checkFilter(Item item, String searchText, String category) {
        boolean matchesCategory = category == null
                || "Tất cả".equals(category)
                || item.getType().name().equalsIgnoreCase(category);
        boolean matchesSearch = true;
        if (searchText != null && !searchText.isEmpty()) {
            String lowerCaseFilter = searchText.toLowerCase();
            String itemCode = String.format("SP-%04d", item.getId()).toLowerCase();
            matchesSearch = item.getName().toLowerCase().contains(lowerCaseFilter)
                    || itemCode.contains(lowerCaseFilter);
        }
        return matchesCategory && matchesSearch;
    }

    private void setupActionColumn() {
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnPublish = createStyledButton(" Đăng Bán ", "#10b981", "#059669");
            private final Button btnEdit = createStyledButton(" Sửa ", "#f59e0b", "#d97706");
            private final Button btnDelete = createStyledButton(" Xoá ", "#ef4444", "#dc2626");
            private final Button btnView = createStyledButton(" Xem Chi Tiết ", "#3b82f6", "#2563eb");
            private final Button btnConfirmPaid = createStyledButton(" Xác Nhận ", "#10b981", "#059669");
            private final Label lblPaidStatus = new Label("💰 Đã Thanh Toán");
            
            private final HBox pane = new HBox(8, btnPublish, btnEdit, btnDelete,
                    btnView, btnConfirmPaid, lblPaidStatus);

            {
                // Làm đẹp cho Label trạng thái
                lblPaidStatus.setStyle("-fx-background-color: #d1fae5; -fx-text-fill: #065f46; -fx-padding: 5 12; -fx-background-radius: 12; -fx-font-weight: bold; -fx-font-size: 12px;");
                pane.setStyle("-fx-alignment: center;");

                btnPublish.setOnAction(e -> handlePublish(getCurrentRowItem()));
                btnEdit.setOnAction(e -> handleEdit(getCurrentRowItem()));
                btnDelete.setOnAction(e -> handleDelete(getCurrentRowItem()));
                btnView.setOnAction(e -> handleViewDetail(getCurrentRowItem()));
                btnConfirmPaid.setOnAction(e -> handleConfirmPaid(getCurrentRowItem()));
            }

            // Hàm tiện ích tạo nút bấm chuyên nghiệp (có Hover)
            private Button createStyledButton(String text, String color, String hoverColor) {
                Button btn = new Button(text);
                String baseStyle = "-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6; -fx-padding: 5 10;";
                String hoverStyle = "-fx-background-color: " + hoverColor + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6; -fx-padding: 5 10;";
                
                btn.setStyle(baseStyle);
                btn.setOnMouseEntered(e -> btn.setStyle(hoverStyle));
                btn.setOnMouseExited(e -> btn.setStyle(baseStyle));
                return btn;
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }

                Item currentItem = getTableView().getItems().get(getIndex());
                String status = getAuctionStatus(currentItem);
                hideAll();

                if ("PAID".equals(status)) {
                    lblPaidStatus.setVisible(true);
                    lblPaidStatus.setManaged(true);
                    showButtons(btnView);
                } else if ("FINISHED".equals(status)) {
                    showButtons(btnView, btnConfirmPaid);
                } else if (!hasAuction(currentItem) || "CANCELLED".equals(status)) {
                    showButtons(btnPublish, btnEdit, btnDelete);
                } else {
                    showButtons(btnView);
                }
                setGraphic(pane);
            }

            private Item getCurrentRowItem() {
                return getTableView().getItems().get(getIndex());
            }

            private void hideAll() {
                for (javafx.scene.Node node : pane.getChildren()) {
                    node.setVisible(false);
                    node.setManaged(false);
                }
            }

            private void showButtons(Button... buttons) {
                for (Button button : buttons) {
                    button.setVisible(true);
                    button.setManaged(true);
                }
            }
        });
    }
    // =================================================================================

    @FXML
    void handleAddNewItem(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/nhom3/client/view/add_item.fxml"));
            Parent root = loader.load();
            Stage popupStage = new Stage();
            popupStage.getIcons().add(new javafx.scene.image.Image(getClass().getResourceAsStream("/images/icon.png")));
            popupStage.setTitle("Thêm Sản Phẩm Mới");
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setScene(new Scene(root));
            popupStage.setResizable(false);
            popupStage.showAndWait();
            loadSellerItems();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống", "Không thể mở cửa sổ thêm sản phẩm!");
        }
    }

    private void handlePublish(Item item) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/nhom3/client/view/publish_auction.fxml"));
            Parent root = loader.load();
            PublishAuctionController controller = loader.getController();
            controller.setItem(item);

            Stage stage = new Stage();
            stage.getIcons().add(new javafx.scene.image.Image(getClass().getResourceAsStream("/images/icon.png")));
            stage.setScene(new Scene(root));
            stage.setTitle("Đăng Bán Sản Phẩm");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
            loadSellerItems();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleEdit(Item item) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/nhom3/client/view/add_item.fxml"));
            Parent root = loader.load();
            AddItemController controller = loader.getController();
            controller.setEditingItem(item);

            Stage popupStage = new Stage();
            popupStage.getIcons().add(new javafx.scene.image.Image(getClass().getResourceAsStream("/images/icon.png")));
            popupStage.setTitle("Sửa Sản Phẩm: " + item.getName());
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setScene(new Scene(root));
            popupStage.setResizable(false);
            popupStage.showAndWait();
            loadSellerItems();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleDelete(Item item) {
        User currentUser = UserSession.getInstance().getLoggedInUser();
        if (!(currentUser instanceof Seller)) {
            showAlert(Alert.AlertType.ERROR, "Từ chối", "Bạn không có quyền thực hiện thao tác này!");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Bạn có chắc chắn muốn xóa sản phẩm: " + item.getName() + "?",
                ButtonType.OK, ButtonType.CANCEL);
        alert.setHeaderText(null);
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            pendingDeleteItem = item;
            try {
                ItemActionPayload payload = new ItemActionPayload(item.getId(), currentUser.getId());
                ServerConnection.getInstance().sendMessage(new Packet(PacketType.DELETE_ITEM, payload));
            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Lỗi mạng", "Không thể gửi yêu cầu xóa sản phẩm!");
            }
        }
    }

    private void handleViewDetail(Item item) {
        pendingViewItem = item;
        if (item.getImageBase64() == null || item.getImageBase64().isEmpty()) {
            requestItemImage(item.getId());
        }
        try {
            ItemActionPayload payload = new ItemActionPayload(item.getId(), 0);
            ServerConnection.getInstance().sendMessage(new Packet(PacketType.LOAD_AUCTION_BY_ITEM, payload));
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi mạng", "Không thể tải chi tiết phiên đấu giá!");
        }
    }

    private void handleConfirmPaid(Item item) {
        User currentUser = UserSession.getInstance().getLoggedInUser();
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Xác nhận đã nhận đủ tiền cho sản phẩm: " + item.getName() + "?",
                ButtonType.YES, ButtonType.NO);
        alert.setHeaderText(null);
        if (currentUser != null && alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            try {
                ItemActionPayload payload = new ItemActionPayload(item.getId(), currentUser.getId());
                ServerConnection.getInstance().sendMessage(new Packet(PacketType.CONFIRM_PAYMENT, payload));
            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Lỗi mạng", "Không thể gửi yêu cầu xác nhận!");
            }
        }
    }

    private void openDetailWindow(Item item, Auction auction, String status) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/nhom3/client/view/view_item_detail.fxml"));
            Parent root = loader.load();
            ViewItemDetailController controller = loader.getController();
            controller.setItemData(item, auction, status);

            Stage stage = new Stage();
            stage.getIcons().add(new javafx.scene.image.Image(getClass().getResourceAsStream("/images/icon.png")));
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Chi Tiết Đấu Giá: " + item.getName());
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Auction toAuction(AuctionListResponsePayload.AuctionDTO dto) {
        ItemType type = ItemType.valueOf(dto.itemType);
        Item item = type.createItem(dto.itemId, dto.itemName, dto.startPrice);
        item.setCurHighest(dto.curHighest);
        item.setImageBase64(dto.imageBase64);
        Auction auction = new Auction(
                dto.auctionId,
                item,
                LocalDateTime.parse(dto.startTime),
                LocalDateTime.parse(dto.endTime));
        auction.setBidStep(dto.bidStep);
        auction.setStatus(StatusOfAuction.valueOf(dto.status));
        if (dto.highestBidderId > 0) {
            auction.setHighestBidder(new Bidder(dto.highestBidderId, null, null));
        }
        return auction;
    }

    private boolean hasAuction(Item item) {
        return !getAuctionStatus(item).isEmpty();
    }

    private String getAuctionStatus(Item item) {
        return itemStatusMap.getOrDefault(item.getId(), "");
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
