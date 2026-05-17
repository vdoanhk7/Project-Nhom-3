package com.nhom3.client.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.time.temporal.ChronoUnit;

import com.nhom3.client.event.ClientEventBus;
import com.nhom3.client.event.ClientEvents;
import com.nhom3.client.event.ControllerLifecycle;
import com.nhom3.client.network.ServerConnection;
import com.nhom3.client.utils.UserSession;
import com.nhom3.shared.model.auction.Auction;
import com.nhom3.shared.model.auction.StatusOfAuction;
import com.nhom3.shared.model.item.Item;
import com.nhom3.shared.model.item.ItemType;
import com.nhom3.shared.model.user.Role;
import com.nhom3.shared.model.user.User;
import com.nhom3.shared.model.user.Bidder;
import com.nhom3.shared.network.packet.Packet;
import com.nhom3.shared.network.packet.PacketType;
import com.nhom3.shared.network.payload.AuctionListResponsePayload;
import com.nhom3.shared.network.payload.ItemActionPayload;
import com.nhom3.shared.network.payload.ItemImagePayload;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class MarketController {

    @FXML private FlowPane flowMarket;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cbCategory;
    @FXML private Button btnReload;

    private static final ExecutorService IMAGE_LOADER = Executors.newFixedThreadPool(4, runnable -> {
        Thread thread = new Thread(runnable, "Market-ImageLoader");
        thread.setDaemon(true);
        return thread;
    });
    
    // --- 2 BIẾN MỚI CHO TÍNH NĂNG LỌC ---
    @FXML private TextField txtSellerName; 
    @FXML private ComboBox<String> cbTimeLeft; 

    private List<Auction> allActiveAuctions = new ArrayList<>();
    private final Map<Integer, String> imageCache = new ConcurrentHashMap<>();
    private final Set<Integer> requestedImageIds = ConcurrentHashMap.newKeySet();
    private final Set<Integer> noImageIds = ConcurrentHashMap.newKeySet();
    private final Map<Integer, StackPane> imageSlots = new ConcurrentHashMap<>();

    @FXML
    public void initialize() {
        ClientEventBus eventBus = ClientEventBus.getDefault();
        eventBus.subscribe(ClientEvents.ActiveAuctionsLoaded.class, this, MarketController::handleActiveAuctionsLoaded);
        eventBus.subscribe(ClientEvents.ItemImageLoaded.class, this, MarketController::handleItemImageLoaded);
        ControllerLifecycle.unsubscribeOnDetach(flowMarket, this);
        List<String> categories = new ArrayList<>();
        categories.add("Tất Cả");
        categories.addAll(java.util.Arrays.stream(ItemType.values()).map(Enum::name).toList());
        cbCategory.setItems(FXCollections.observableArrayList(categories));
        cbCategory.setValue("Tất Cả");

        // Cấu hình ComboBox Thời gian còn lại (Bảo vệ NPE nếu FXML chưa kịp thêm)
        if (cbTimeLeft != null) {
            cbTimeLeft.setItems(FXCollections.observableArrayList("Tất Cả", "Dưới 1 giờ", "Dưới 24 giờ", "Trên 24 giờ"));
            cbTimeLeft.setValue("Tất Cả");
            cbTimeLeft.valueProperty().addListener((obs, oldV, newV) -> filterMarket());
        }

        if (txtSellerName != null) {
            txtSellerName.textProperty().addListener((obs, oldV, newV) -> filterMarket());
        }

        btnReload.setOnMouseEntered(e -> btnReload.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-weight: bold; -fx-font-size: 14px;"));
        btnReload.setOnMouseExited(e -> btnReload.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-weight: bold; -fx-font-size: 14px;"));

        btnReload.setOnAction(e -> {
            txtSearch.clear();
            cbCategory.setValue("Tất Cả");
            if(txtSellerName != null) txtSellerName.clear();
            if(cbTimeLeft != null) cbTimeLeft.setValue("Tất Cả");
            loadMarket();
        });
        
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> filterMarket());
        cbCategory.valueProperty().addListener((observable, oldValue, newValue) -> filterMarket());
        loadMarket();
    }

    @FXML
    public void loadMarket() {
        try {
            ServerConnection.getInstance().sendMessage(new Packet(PacketType.LOAD_ACTIVE_AUCTIONS, null));
        } catch (Exception e) {
            e.printStackTrace();
            showEmptyMessage("Không thể tải dữ liệu chợ đấu giá.");
        }
    }

    public void handleLoadActiveAuctionsResult(List<AuctionListResponsePayload.AuctionDTO> dtoList) {
        allActiveAuctions = new ArrayList<>();
        requestedImageIds.clear();
        imageSlots.clear();
        imageCache.clear();
        noImageIds.clear();
        if (dtoList != null) {
            for (AuctionListResponsePayload.AuctionDTO dto : dtoList) {
                allActiveAuctions.add(toAuction(dto));
            }
        }
        javafx.application.Platform.runLater(this::filterMarket);
    }

    private void handleActiveAuctionsLoaded(ClientEvents.ActiveAuctionsLoaded event) {
        handleLoadActiveAuctionsResult(event.auctions());
    }

    public void handleItemImageResult(ItemImagePayload payload) {
        if (payload == null || payload.getItemId() <= 0) {
            return;
        }

        String imageBase64 = payload.getImageBase64();
        if (imageBase64 == null || imageBase64.isEmpty()) {
            noImageIds.add(payload.getItemId());
            StackPane imageSlot = imageSlots.get(payload.getItemId());
            if (imageSlot != null) {
                showImagePlaceholder(imageSlot, "No Image");
            }
            return;
        }

        imageCache.put(payload.getItemId(), imageBase64);
        for (Auction auction : allActiveAuctions) {
            if (auction.getItem() != null && auction.getItem().getId() == payload.getItemId()) {
                auction.getItem().setImageBase64(imageBase64);
                break;
            }
        }

        StackPane imageSlot = imageSlots.get(payload.getItemId());
        if (imageSlot != null) {
            renderImage(imageSlot, imageBase64);
        }
    }

    private void handleItemImageLoaded(ClientEvents.ItemImageLoaded event) {
        handleItemImageResult(event.payload());
    }

    private Auction toAuction(AuctionListResponsePayload.AuctionDTO dto) {
        ItemType type = ItemType.valueOf(dto.itemType);
        Item item = type.createItem(dto.itemId, dto.itemName, dto.startPrice);
        item.setDescription(dto.itemDescription);
        item.setCurHighest(dto.curHighest);
        item.setImageBase64(imageCache.getOrDefault(dto.itemId, dto.imageBase64));
        item.setSellerName(dto.sellerName); // Lấy tên người bán từ Server
        
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

    private void filterMarket() {
        flowMarket.getChildren().clear();
        imageSlots.clear();

        if (allActiveAuctions == null || allActiveAuctions.isEmpty()) {
            showEmptyMessage("Hiện tại chưa có sản phẩm nào đang lên sàn.");
            return;
        }

        String searchText = txtSearch.getText().toLowerCase();
        String selectedCategory = cbCategory.getValue();
        
        // Giá trị của các filter mới
        String searchSeller = txtSellerName != null ? txtSellerName.getText().toLowerCase().trim() : "";
        String selectedTime = cbTimeLeft != null ? cbTimeLeft.getValue() : "Tất Cả";

        int displayCount = 0;

        for (Auction auction : allActiveAuctions) {
            Item item = auction.getItem();
            
            // Lọc Danh mục
            boolean matchCategory = "Tất Cả".equals(selectedCategory)
                    || item.getType().name().equalsIgnoreCase(selectedCategory);
                    
            // Lọc Tên Sản phẩm
            boolean matchSearch = searchText.isEmpty()
                    || item.getName().toLowerCase().contains(searchText);

            // Lọc Tên Người Bán
            boolean matchSeller = searchSeller.isEmpty() || 
                    (item.getSellerName() != null && item.getSellerName().toLowerCase().contains(searchSeller));

            // Lọc Thời Gian Còn Lại
            boolean matchTime = true;
            if (!"Tất Cả".equals(selectedTime) && auction.getEndTime() != null) {
                long hoursLeft = ChronoUnit.HOURS.between(LocalDateTime.now(), auction.getEndTime());
                if ("Dưới 1 giờ".equals(selectedTime)) {
                    matchTime = hoursLeft < 1 && hoursLeft >= 0;
                } else if ("Dưới 24 giờ".equals(selectedTime)) {
                    matchTime = hoursLeft < 24 && hoursLeft >= 0;
                } else if ("Trên 24 giờ".equals(selectedTime)) {
                    matchTime = hoursLeft >= 24;
                }
            }

            if (matchCategory && matchSearch && matchSeller && matchTime) {
                flowMarket.getChildren().add(createProductCard(auction));
                displayCount++;
            }
        }

        if (displayCount == 0) {
            showEmptyMessage("Không tìm thấy sản phẩm phù hợp.");
        }
    }

    private void showEmptyMessage(String message) {
        Label lblEmpty = new Label(message);
        lblEmpty.setStyle("-fx-text-fill: #95a5a6; -fx-font-style: italic; -fx-font-size: 18px;");
        
        VBox emptyBox = new VBox(lblEmpty);
        emptyBox.setAlignment(Pos.CENTER);
        emptyBox.setPrefWidth(800);
        emptyBox.setPadding(new Insets(50, 0, 0, 0));
        
        flowMarket.getChildren().add(emptyBox);
    }

    private VBox createProductCard(Auction auction) {
        Item item = auction.getItem();

        VBox card = new VBox(12);
        String defaultStyle = "-fx-background-color: white; -fx-background-radius: 12; "
                + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 10, 0, 0, 4);";
        String hoverStyle = "-fx-background-color: white; -fx-background-radius: 12; "
                + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 15, 0, 0, 6);";
        
        card.setStyle(defaultStyle);
        card.setPadding(new Insets(15));
        card.setPrefWidth(240);
        card.setPrefHeight(360);

        card.setOnMouseEntered(e -> {
            card.setStyle(hoverStyle);
            card.setTranslateY(-4); 
        });
        card.setOnMouseExited(e -> {
            card.setStyle(defaultStyle);
            card.setTranslateY(0); 
        });

        StackPane imageWrapper = new StackPane();
        imageWrapper.setPrefSize(210, 160);
        imageWrapper.setStyle("-fx-background-color: linear-gradient(to bottom right, #f1f2f6, #dfe4ea); -fx-background-radius: 8;");

        if (item.getImageBase64() != null && !item.getImageBase64().isEmpty()) {
            renderImage(imageWrapper, item.getImageBase64());
        } else if (noImageIds.contains(item.getId())) {
            showImagePlaceholder(imageWrapper, "No Image");
        } else {
            showImagePlaceholder(imageWrapper, "Đang tải...");
            imageSlots.put(item.getId(), imageWrapper);
            requestItemImage(item.getId());
        }

        Label lblType = new Label(item.getType().name());
        lblType.setStyle("-fx-background-color: #e8f4f8; -fx-text-fill: #2980b9; "
                + "-fx-padding: 3 8 3 8; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;");
        
        Label lblName = new Label(item.getName());
        lblName.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #2d3436;");
        lblName.setWrapText(true);
        lblName.setMaxHeight(45); 
        lblName.setMinHeight(45);
        lblName.setAlignment(Pos.TOP_LEFT);
        
        // Hiển thị tên người bán
        String sellerStr = item.getSellerName() != null && !item.getSellerName().isEmpty() ? item.getSellerName() : "Ẩn danh";
        Label lblSeller = new Label("👤 " + sellerStr);
        lblSeller.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px; -fx-font-style: italic;");

        VBox priceBox = new VBox(2);
        Label lblPriceTitle = new Label("Giá cao nhất hiện tại:");
        lblPriceTitle.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");
        Label lblPrice = new Label(String.format("%,.0f VNĐ", item.getCurHighest()));
        lblPrice.setStyle("-fx-font-weight: bold; -fx-font-size: 19px; -fx-text-fill: #e74c3c;");
        priceBox.getChildren().addAll(lblPriceTitle, lblPrice);

        VBox spacer = new VBox();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button btnBid = new Button(getActionButtonText());
        btnBid.setMaxWidth(Double.MAX_VALUE);
        String btnDefaultStyle = "-fx-background-color: #3498db; -fx-text-fill: white; "
                + "-fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6; -fx-padding: 8 0;";
        String btnHoverStyle = "-fx-background-color: #2980b9; -fx-text-fill: white; "
                + "-fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6; -fx-padding: 8 0;";
        
        btnBid.setStyle(btnDefaultStyle);
        btnBid.setOnMouseEntered(e -> btnBid.setStyle(btnHoverStyle));
        btnBid.setOnMouseExited(e -> btnBid.setStyle(btnDefaultStyle));
        btnBid.setOnAction(e -> openItemDetail(item, auction, this::loadMarket));

        // Thêm lblSeller vào Giao diện Card
        card.getChildren().addAll(imageWrapper, lblType, lblName, lblSeller, priceBox, spacer, btnBid);
        
        return card;
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

    private void renderImage(StackPane imageWrapper, String imageBase64) {
        imageWrapper.getChildren().clear();
        try {
            byte[] imageBytes = Base64.getDecoder().decode(imageBase64);
            Image img = new Image(new ByteArrayInputStream(imageBytes));
            ImageView imageView = new ImageView(img);
            imageView.setFitWidth(210);
            imageView.setFitHeight(160);
            imageView.setPreserveRatio(false);

            Rectangle clip = new Rectangle(210, 160);
            clip.setArcWidth(16);
            clip.setArcHeight(16);
            imageView.setClip(clip);
            imageWrapper.getChildren().add(imageView);
        } catch (Exception e) {
            showImagePlaceholder(imageWrapper, "Không có ảnh");
        }
    }

    private void showImagePlaceholder(StackPane imageWrapper, String text) {
        imageWrapper.getChildren().clear();
        Label placeholder = new Label(text);
        placeholder.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 13px; -fx-font-weight: bold;");
        imageWrapper.getChildren().add(placeholder);
    }

    private String getActionButtonText() {
        User currentUser = UserSession.getInstance().getLoggedInUser();
        if (currentUser != null && currentUser.getRole() == Role.BIDDER) {
            return " Vào Đấu Giá ";
        }
        return " Xem Phiên Đấu Giá ";
    }

    public void openItemDetail(Item item, Auction auction, Runnable onWindowClosed) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/nhom3/client/view/view_item_detail.fxml"));
            Parent root = loader.load();

            ViewItemDetailController detailController = loader.getController();
            detailController.setItemData(item, auction, auction.getStatus().name());

            Stage stage = new Stage();
            stage.getIcons().add(new javafx.scene.image.Image(getClass().getResourceAsStream("/images/icon.png")));
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
