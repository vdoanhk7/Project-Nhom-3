package com.nhom3.client.controller;

import com.nhom3.server.dao.AuctionDAO;
import com.nhom3.server.dao.AuctionDAOImpl;
import com.nhom3.server.dao.UserDAO;
import com.nhom3.server.dao.UserDAOImpl;
import com.nhom3.shared.model.auction.Auction;
import com.nhom3.shared.model.user.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.application.Platform;

import java.util.List;

public class AdminDashboardController {

    // Các thành phần Quản lý Người dùng
    @FXML private TableView<User> tableUsers;
    @FXML private TableColumn<User, Integer> colUserId;
    @FXML private TableColumn<User, String> colUserName;
    @FXML private TableColumn<User, String> colUserFullName;
    @FXML private TableColumn<User, String> colUserRole;

    // Các thành phần Quản lý Đấu giá
    @FXML private TableView<Auction> tableAuctions;
    @FXML private TableColumn<Auction, Integer> colAuctionId;
    @FXML private TableColumn<Auction, String> colItemName;
    @FXML private TableColumn<Auction, Double> colCurrentPrice;
    @FXML private TableColumn<Auction, String> colStatus;

    private UserDAO userDAO = new UserDAOImpl();
    private AuctionDAO auctionDAO = new AuctionDAOImpl();

    @FXML
    public void initialize() {
        setupUserTable();
        setupAuctionTable();

        // Tải dữ liệu ban đầu
        loadUserData();
        loadAuctionData();
    }

    private void setupUserTable() {
        colUserId.setCellValueFactory(new PropertyValueFactory<>("id"));
        // Giả sử UserInfo có các thuộc tính này hoặc bạn lấy từ User model
        colUserName.setCellValueFactory(cellData ->
                javafx.beans.binding.Bindings.createStringBinding(() -> cellData.getValue().getUserInfo().getUserName()));
        colUserFullName.setCellValueFactory(cellData ->
                javafx.beans.binding.Bindings.createStringBinding(() -> cellData.getValue().getUserInfo().getName()));
        colUserRole.setCellValueFactory(new PropertyValueFactory<>("role"));
    }

    private void setupAuctionTable() {
        colAuctionId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colItemName.setCellValueFactory(cellData ->
                javafx.beans.binding.Bindings.createStringBinding(() -> cellData.getValue().getItem().getName()));
        colCurrentPrice.setCellValueFactory(cellData ->
                javafx.beans.binding.Bindings.createDoubleBinding(() -> cellData.getValue().getItem().getCurHighest()).asObject());
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
    }

    private void loadUserData() {
    }

    private void loadAuctionData() {
        List<Auction> auctions = auctionDAO.getActiveAuctions(); // Tận dụng hàm sẵn có [cite: 504, 547]
        tableAuctions.setItems(FXCollections.observableArrayList(auctions));
    }

    @FXML
    private void handleLogout() {
        // Logic quay lại màn hình Login [cite: 5]
        System.out.println("Admin đã đăng xuất.");
    }

    @FXML
    private void handleCancelAuction() {
        Auction selected = tableAuctions.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // Gọi hàm hủy phiên nếu phát hiện gian lận [cite: 27, 40]
            // boolean success = auctionDAO.updateStatus(selected.getId(), "CANCELLED");
            System.out.println("Admin đã hủy phiên: " + selected.getId());
            loadAuctionData(); // Refresh lại bảng
        }
    }
}