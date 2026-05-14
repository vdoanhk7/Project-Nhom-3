package com.nhom3.client.controller;

import java.util.List;

import com.nhom3.client.utils.UserSession;
import com.nhom3.shared.model.auction.Auction;
import com.nhom3.shared.model.auction.StatusOfAuction;
import com.nhom3.shared.model.item.Item;
import com.nhom3.shared.model.user.Role;
import com.nhom3.shared.model.user.User;
import com.nhom3.server.dao.AuctionDAO;
import com.nhom3.server.dao.AuctionDAOImpl;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;

@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
        value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD",
        justification = "JavaFX controllers are reached from the socket dispatcher through the active screen instance.")
public class MarketController {

    @FXML private FlowPane flowMarket;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cbCategory;
    @FXML private Button btnReload;

    private List<Auction> allActiveAuctions = new ArrayList<>();
    private static MarketController instance;

    public static MarketController getInstance() {
        return instance;
    }

    @FXML
    public void initialize() {
        instance = this;
        cbCategory.setItems(FXCollections.observableArrayList("Tat ca", "ART", "ELECTRONICS", "VEHICLE"));
        cbCategory.setValue("Tat ca");

        btnReload.setOnAction(e -> {
            txtSearch.clear();
            cbCategory.setValue("Tat ca");
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
            showEmptyMessage("Khong the tai du lieu cho dau gia.");
        }
    }

    public void handleLoadActiveAuctionsResult(List<AuctionListResponsePayload.AuctionDTO> dtoList) {
        allActiveAuctions = new ArrayList<>();
        if (dtoList != null) {
            for (AuctionListResponsePayload.AuctionDTO dto : dtoList) {
                allActiveAuctions.add(toAuction(dto));
            }
        }
        filterMarket();
    }

    private void filterMarket() {
        flowMarket.getChildren().clear();

        if (allActiveAuctions == null || allActiveAuctions.isEmpty()) {
            showEmptyMessage("Hien tai chua co san pham nao dang len san.");
            return;
        }

        String searchText = txtSearch.getText().toLowerCase();
        String selectedCategory = cbCategory.getValue();
        int displayCount = 0;

        for (Auction auction : allActiveAuctions) {
            Item item = auction.getItem();
            boolean matchCategory = "Tat ca".equals(selectedCategory)
                    || item.getType().name().equalsIgnoreCase(selectedCategory);
            boolean matchSearch = searchText.isEmpty()
                    || item.getName().toLowerCase().contains(searchText);

            if (matchCategory && matchSearch) {
                flowMarket.getChildren().add(createProductCard(auction));
                displayCount++;
            }
        }

        if (displayCount == 0) {
            showEmptyMessage("Khong tim thay san pham phu hop.");
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
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; "
                + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 4);");
        card.setPadding(new Insets(15));
        card.setPrefWidth(220);

        Rectangle imagePlaceholder = new Rectangle(190, 140);
        imagePlaceholder.setStyle("-fx-fill: #ecf0f1; -fx-arc-width: 10; -fx-arc-height: 10;");

        Label lblName = new Label(item.getName());
        lblName.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        Label lblType = new Label(item.getType().name());
        lblType.setStyle("-fx-text-fill: #7f8c8d;");
        Label lblPrice = new Label(String.format("%,.0f VND", item.getCurHighest()));
        lblPrice.setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-text-fill: #e74c3c;");

        // Nút bấm
        Button btnBid = new Button(getActionButtonText());
        btnBid.setMaxWidth(Double.MAX_VALUE);
        btnBid.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; "
                + "-fx-font-weight: bold; -fx-cursor: hand;");
        btnBid.setOnAction(e -> openItemDetail(item, auction, this::loadMarket));

        card.getChildren().addAll(imagePlaceholder, lblName, lblType, lblPrice, btnBid);
        return card;
    }

    private String getActionButtonText() {
        User currentUser = UserSession.getInstance().getLoggedInUser();
        if (currentUser != null && currentUser.getRole() == Role.BIDDER) {
            return "Vào đấu giá";
        }
        return "Xem phiên đấu giá";
    }

    // Hàm mở Modal Chi tiết
    public void openItemDetail(Item item, Auction auction, Runnable onWindowClosed) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/nhom3/client/view/view_item_detail.fxml"));
            Parent root = loader.load();

            ViewItemDetailController detailController = loader.getController();
            detailController.setItemData(item, auction, auction.getStatus().name());

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Chi tiet san pham - " + item.getName());
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
