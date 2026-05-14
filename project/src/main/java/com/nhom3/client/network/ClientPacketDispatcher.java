package com.nhom3.client.network;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;
import com.nhom3.shared.network.packet.Packet;
import com.nhom3.shared.network.packet.PacketType;
import com.nhom3.shared.network.payload.*;
import com.nhom3.shared.model.user.*;
import javafx.scene.control.Alert;
import com.nhom3.client.controller.*;

public class ClientPacketDispatcher {
    private static final Logger logger = LoggerFactory.getLogger(ClientPacketDispatcher.class);
    private final Map<PacketType, ClientPacketHandler> handlers = new HashMap<>();

    public ClientPacketDispatcher() {
        registerHandlers();
    }

    private void registerHandlers() {
        handlers.put(PacketType.LOGIN, this::handleLogin);
        handlers.put(PacketType.REGISTER, this::handleRegister);
        handlers.put(PacketType.PLACE_BID, this::handlePlaceBid);
        handlers.put(PacketType.LOAD_BID_HISTORY, this::handleLoadBidHistory);
        handlers.put(PacketType.LOAD_SELLER_ITEMS, this::handleLoadSellerItems);
        handlers.put(PacketType.LOAD_ACTIVE_AUCTIONS, this::handleLoadActiveAuctions);
        handlers.put(PacketType.LOAD_AUCTION_BY_ITEM, this::handleLoadAuctionByItem);
        handlers.put(PacketType.SAVE_ITEM, this::handleItemMutation);
        handlers.put(PacketType.UPDATE_ITEM, this::handleItemMutation);
        handlers.put(PacketType.DELETE_ITEM, this::handleDeleteItem);
        handlers.put(PacketType.CONFIRM_PAYMENT, this::handleConfirmPayment);
        handlers.put(PacketType.CANCEL_AUCTION, this::handleCancelAuction);
        handlers.put(PacketType.UPDATE_PROFILE, this::handleUpdateProfile);
        handlers.put(PacketType.CHANGE_PASSWORD, this::handleChangePassword);
        handlers.put(PacketType.LOAD_USERS, this::handleLoadUsers);
        handlers.put(PacketType.PUBLISH_AUCTION, this::handlePublishAuction);
        handlers.put(PacketType.LOAD_PURCHASE_HISTORY, this::handleLoadPurchaseHistory);
        handlers.put(PacketType.PLACE_AUTO_BID, this::handlePlaceAutoBid);
        handlers.put(PacketType.CHECK_AUTO_BID, this::handleCheckAutoBid);
        handlers.put(PacketType.CANCEL_AUTO_BID, this::handleCancelAutoBid);
        handlers.put(PacketType.LOAD_DASHBOARD, this::handleLoadDashboard);
        handlers.put(PacketType.SCREEN_NOTIFY, this::handleScreenNotify);
        handlers.put(PacketType.AUCTION_SUBSCRIBE, this::handleAuctionSubscribe);
    }

    public void dispatch(Packet response, Gson gson) {
        ClientPacketHandler handler = handlers.get(response.getType());
        if (handler != null) {
            try {
                handler.handle(response, gson);
            } catch (Exception e) {
                logger.error("Lỗi khi xử lý gói tin Client: " + response.getType(), e);
            }
        } else {
            logger.warn("Loại gói tin không xác định: {}", response.getType());
        }
    }

    private void handleLogin(Packet response, Gson gson) {
        ResultPayload loginResult = gson.fromJson(response.getPayload(), ResultPayload.class);
        logger.info("Server phản hồi Đăng nhập: {}", loginResult.getResult());
        try {
            if (LoginController.getInstance() != null) {
                User loggedInUser = null;
                if (loginResult.getResult()) {
                    UserInfo info = new UserInfo(loginResult.getUsername(), "", loginResult.getFullName());
                    UserContact contact = new UserContact(loginResult.getEmail(), loginResult.getPhone());
                    if ("BIDDER".equals(loginResult.getRole())) {
                        loggedInUser = new Bidder(loginResult.getUserId(), info, contact);
                    } else if ("SELLER".equals(loginResult.getRole())) {
                        loggedInUser = new Seller(loginResult.getUserId(), info, contact);
                    } else {
                        loggedInUser = new Admin(loginResult.getUserId(), info, contact);
                    }
                }
                LoginController.getInstance().handleLoginResult(loginResult.getResult(), loggedInUser);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleRegister(Packet response, Gson gson) {
        ResultPayload regResultPayload = gson.fromJson(response.getPayload(), ResultPayload.class);
        logger.info("Server phản hồi Đăng ký: {}", regResultPayload.getResult());
        try {
            if (SignupController.getInstance() != null) {
                SignupController.getInstance().handleSignupResult(regResultPayload.getResult(), regResultPayload.getMessage());
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void handlePlaceBid(Packet response, Gson gson) {
        ResultPayload bidRes = gson.fromJson(response.getPayload(), ResultPayload.class);
        logger.info("Server phản hồi Đặt giá: {}", bidRes.getResult());
        try {
            if (ViewItemDetailController.getInstance() != null) {
                ViewItemDetailController.getInstance().handleBidResult(bidRes.getResult(), bidRes.getMessage());
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void handleLoadBidHistory(Packet response, Gson gson) {
        BidHistoryResponsePayload histResult = gson.fromJson(response.getPayload(), BidHistoryResponsePayload.class);
        try {
            if (ViewItemDetailController.getInstance() != null) {
                ViewItemDetailController.getInstance().handleLoadHistoryResult(histResult.getAuctionId(), histResult.getHistoryList());
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void handleLoadSellerItems(Packet response, Gson gson) {
        SellerItemsResponsePayload itemsResult = gson.fromJson(response.getPayload(), SellerItemsResponsePayload.class);
        try {
            if (ManageItemController.getInstance() != null) {
                ManageItemController.getInstance().handleLoadItemsResult(itemsResult.getItems());
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void handleLoadActiveAuctions(Packet response, Gson gson) {
        AuctionListResponsePayload payload =
                gson.fromJson(response.getPayload(), AuctionListResponsePayload.class);
        try {
            if (MarketController.getInstance() != null) {
                MarketController.getInstance().handleLoadActiveAuctionsResult(payload.getAuctions());
            }
            if (AdminDashboardController.getInstance() != null) {
                AdminDashboardController.getInstance().handleLoadAuctionDataResult(payload.getAuctions());
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void handleLoadAuctionByItem(Packet response, Gson gson) {
        AuctionListResponsePayload payload =
                gson.fromJson(response.getPayload(), AuctionListResponsePayload.class);
        try {
            if (ManageItemController.getInstance() != null) {
                ManageItemController.getInstance().handleLoadAuctionByItemResult(payload.getAuctions());
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void handleItemMutation(Packet response, Gson gson) {
        ResultPayload result = gson.fromJson(response.getPayload(), ResultPayload.class);
        try {
            if (AddItemController.getInstance() != null) {
                AddItemController.getInstance().handleItemMutationResult(
                        response.getType(), result.getResult(), result.getMessage());
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void handleDeleteItem(Packet response, Gson gson) {
        ResultPayload result = gson.fromJson(response.getPayload(), ResultPayload.class);
        try {
            if (ManageItemController.getInstance() != null) {
                ManageItemController.getInstance().handleDeleteItemResult(
                        result.getResult(), result.getMessage());
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void handleConfirmPayment(Packet response, Gson gson) {
        ResultPayload result = gson.fromJson(response.getPayload(), ResultPayload.class);
        try {
            if (ManageItemController.getInstance() != null) {
                ManageItemController.getInstance().handleConfirmPaymentResult(
                        result.getResult(), result.getMessage());
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void handleCancelAuction(Packet response, Gson gson) {
        ResultPayload result = gson.fromJson(response.getPayload(), ResultPayload.class);
        try {
            if (AdminDashboardController.getInstance() != null) {
                AdminDashboardController.getInstance().handleCancelAuctionResult(
                        result.getResult(), result.getMessage());
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void handleUpdateProfile(Packet response, Gson gson) {
        ResultPayload result = gson.fromJson(response.getPayload(), ResultPayload.class);
        try {
            if (ProfileController.getInstance() != null) {
                ProfileController.getInstance().handleUpdateProfileResult(result);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void handleChangePassword(Packet response, Gson gson) {
        ResultPayload result = gson.fromJson(response.getPayload(), ResultPayload.class);
        try {
            if (ChangePasswordController.getInstance() != null) {
                ChangePasswordController.getInstance().handleChangePasswordResult(
                        result.getResult(), result.getMessage());
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void handleLoadUsers(Packet response, Gson gson) {
        UserListResponsePayload payload = gson.fromJson(response.getPayload(), UserListResponsePayload.class);
        try {
            if (AdminDashboardController.getInstance() != null) {
                AdminDashboardController.getInstance().handleLoadUserDataResult(payload.getUsers());
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void handlePublishAuction(Packet response, Gson gson) {
        ResultPayload pubRes = gson.fromJson(response.getPayload(), ResultPayload.class);
        try {
            if (PublishAuctionController.getInstance() != null) {
                PublishAuctionController.getInstance().handlePublishResult(pubRes.getResult(), pubRes.getMessage());
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void handleLoadPurchaseHistory(Packet response, Gson gson) {
        PurchaseHistoryResponsePayload purHistRes = gson.fromJson(response.getPayload(), PurchaseHistoryResponsePayload.class);
        try {
            if (PurchaseHistoryController.getInstance() != null) {
                PurchaseHistoryController.getInstance().handleLoadHistoryResult(purHistRes.getHistoryList());
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void handlePlaceAutoBid(Packet response, Gson gson) {
        ResultPayload autoRes = gson.fromJson(response.getPayload(), ResultPayload.class);
        Alert alert = new Alert(autoRes.getResult() ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
        alert.setTitle("Auto-Bid");
        alert.setHeaderText(null);
        alert.setContentText(autoRes.getMessage());
        alert.showAndWait();
        if (autoRes.getResult() && ViewItemDetailController.getInstance() != null) {
            ViewItemDetailController.getInstance().checkAutoBidStatus();
        }
    }

    private void handleCheckAutoBid(Packet response, Gson gson) {
        AutoBidPayload config = gson.fromJson(response.getPayload(), AutoBidPayload.class);
        try {
            if (ViewItemDetailController.getInstance() != null) {
                ViewItemDetailController.getInstance().handleCheckAutoBidResult(config);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void handleCancelAutoBid(Packet response, Gson gson) {
        ResultPayload cancelRes = gson.fromJson(response.getPayload(), ResultPayload.class);
        Alert alert = new Alert(cancelRes.getResult() ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
        alert.setTitle("Hủy Auto-Bid");
        alert.setHeaderText(null);
        alert.setContentText(cancelRes.getMessage());
        alert.showAndWait();
        if (cancelRes.getResult() && ViewItemDetailController.getInstance() != null) {
            ViewItemDetailController.getInstance().checkAutoBidStatus();
        }
    }

    private void handleLoadDashboard(Packet response, Gson gson) {
        DashboardResponsePayload dashResult = gson.fromJson(response.getPayload(), DashboardResponsePayload.class);
        try {
            if (DashboardController.getInstance() != null) {
                DashboardController.getInstance().handleDashboardData(dashResult);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void handleScreenNotify(Packet response, Gson gson) {
        ScreenNotifyPayload notifyPayload = gson.fromJson(response.getPayload(), ScreenNotifyPayload.class);
        try {
            if (ViewItemDetailController.getInstance() != null) {
                ViewItemDetailController.getInstance().handleScreenNotify(notifyPayload.getHighestPrice());
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void handleAuctionSubscribe(Packet response, Gson gson) {
        // Subscription acknowledgement. No UI update needed.
    }
}
