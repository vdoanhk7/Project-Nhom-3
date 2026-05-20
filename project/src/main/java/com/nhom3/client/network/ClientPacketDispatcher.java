package com.nhom3.client.network;

import com.google.gson.Gson;
import com.nhom3.client.event.ClientEventBus;
import com.nhom3.client.event.ClientEvents;
import com.nhom3.client.utils.UserSession;
import com.nhom3.shared.model.user.Admin;
import com.nhom3.shared.model.user.Bidder;
import com.nhom3.shared.model.user.Seller;
import com.nhom3.shared.model.user.User;
import com.nhom3.shared.model.user.UserContact;
import com.nhom3.shared.model.user.UserInfo;
import com.nhom3.shared.network.packet.Packet;
import com.nhom3.shared.network.packet.PacketType;
import com.nhom3.shared.network.payload.AuctionListResponsePayload;
import com.nhom3.shared.network.payload.AutoBidPayload;
import com.nhom3.shared.network.payload.BidHistoryResponsePayload;
import com.nhom3.shared.network.payload.DashboardResponsePayload;
import com.nhom3.shared.network.payload.ItemImagePayload;
import com.nhom3.shared.network.payload.PurchaseHistoryResponsePayload;
import com.nhom3.shared.network.payload.ResultPayload;
import com.nhom3.shared.network.payload.ScreenNotifyPayload;
import com.nhom3.shared.network.payload.SellerItemsResponsePayload;
import com.nhom3.shared.network.payload.SystemLogResponsePayload;
import com.nhom3.shared.network.payload.UserListResponsePayload;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientPacketDispatcher {
    private static final Logger logger = LoggerFactory.getLogger(ClientPacketDispatcher.class);

    private final Map<PacketType, ClientPacketHandler> handlers = new HashMap<>();
    private final ClientEventBus eventBus = ClientEventBus.getDefault();

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
        handlers.put(PacketType.LOAD_ALL_AUCTIONS, this::handleLoadAllAuctions);
        handlers.put(PacketType.LOAD_AUCTION_BY_ITEM, this::handleLoadAuctionByItem);
        handlers.put(PacketType.LOAD_ITEM_IMAGE, this::handleLoadItemImage);
        handlers.put(PacketType.SAVE_ITEM, this::handleItemMutation);
        handlers.put(PacketType.UPDATE_ITEM, this::handleItemMutation);
        handlers.put(PacketType.DELETE_ITEM, this::handleDeleteItem);
        handlers.put(PacketType.CONFIRM_PAYMENT, this::handleConfirmPayment);
        handlers.put(PacketType.CANCEL_TRANSACTION, this::handleCancelTransaction);
        handlers.put(PacketType.RATE_SELLER, this::handleRateSeller);
        handlers.put(PacketType.CANCEL_AUCTION, this::handleCancelAuction);
        handlers.put(PacketType.LOAD_PROFILE, this::handleLoadProfile);
        handlers.put(PacketType.UPDATE_PROFILE, this::handleUpdateProfile);
        handlers.put(PacketType.REPUTATION_UPDATED, this::handleReputationUpdated);
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
        handlers.put(PacketType.DELETE_USER, this::handleDeleteUser);
        handlers.put(PacketType.ACCOUNT_DELETED, this::handleAccountDeleted);
        handlers.put(PacketType.SYSTEM_LOGS_RESPONSE, this::handleSystemLogsResponse);
    }

    public void dispatch(Packet response, Gson gson) {
        ClientPacketHandler handler = handlers.get(response.getType());
        if (handler == null) {
            logger.warn("Loại gói tin không xác định: {}", response.getType());
            return;
        }

        try {
            handler.handle(response, gson);
        } catch (Exception e) {
            logger.error("Lỗi khi xử lý gói tin Client: " + response.getType(), e);
        }
    }

    private void handleLogin(Packet response, Gson gson) {
        ResultPayload loginResult = gson.fromJson(response.getPayload(), ResultPayload.class);
        logger.info("Server phản hồi Đăng nhập: {}", loginResult.getResult());
        eventBus.publish(new ClientEvents.LoginResult(loginResult.getResult(), toUser(loginResult)));
    }

    private User toUser(ResultPayload loginResult) {
        if (!loginResult.getResult()) {
            return null;
        }

        UserInfo info = new UserInfo(loginResult.getUsername(), "", loginResult.getFullName());
        info.setProfileImageBase64(loginResult.getProfileImageBase64());
        UserContact contact = new UserContact(loginResult.getEmail(), loginResult.getPhone());
        User user = switch (loginResult.getRole()) {
            case "BIDDER" -> new Bidder(loginResult.getUserId(), info, contact);
            case "SELLER" -> new Seller(loginResult.getUserId(), info, contact);
            default -> new Admin(loginResult.getUserId(), info, contact);
        };
        user.setReputationScore(loginResult.getReputationScore());
        user.setSellerRatingSummary(loginResult.getSellerRatingAverage(), loginResult.getSellerRatingCount());
        return user;
    }

    private void handleRegister(Packet response, Gson gson) {
        ResultPayload result = gson.fromJson(response.getPayload(), ResultPayload.class);
        logger.info("Server phản hồi Đăng ký: {}", result.getResult());
        eventBus.publish(new ClientEvents.SignupResult(result.getResult(), result.getMessage()));
    }

    private void handlePlaceBid(Packet response, Gson gson) {
        ResultPayload result = gson.fromJson(response.getPayload(), ResultPayload.class);
        logger.info("Server phản hồi Đặt giá: {}", result.getResult());
        eventBus.publish(new ClientEvents.BidResult(result.getResult(), result.getMessage()));
    }

    private void handleLoadBidHistory(Packet response, Gson gson) {
        BidHistoryResponsePayload payload = gson.fromJson(response.getPayload(), BidHistoryResponsePayload.class);
        eventBus.publish(new ClientEvents.BidHistoryLoaded(payload.getAuctionId(), payload.getHistoryList()));
    }

    private void handleLoadSellerItems(Packet response, Gson gson) {
        SellerItemsResponsePayload payload = gson.fromJson(response.getPayload(), SellerItemsResponsePayload.class);
        eventBus.publish(new ClientEvents.SellerItemsLoaded(payload.getItems()));
    }

    private void handleLoadActiveAuctions(Packet response, Gson gson) {
        AuctionListResponsePayload payload = gson.fromJson(response.getPayload(), AuctionListResponsePayload.class);
        eventBus.publish(new ClientEvents.ActiveAuctionsLoaded(payload.getAuctions()));
    }

    private void handleLoadAllAuctions(Packet response, Gson gson) {
        AuctionListResponsePayload payload = gson.fromJson(response.getPayload(), AuctionListResponsePayload.class);
        eventBus.publish(new ClientEvents.AllAuctionsLoaded(payload.getAuctions()));
    }

    private void handleLoadAuctionByItem(Packet response, Gson gson) {
        AuctionListResponsePayload payload = gson.fromJson(response.getPayload(), AuctionListResponsePayload.class);
        eventBus.publish(new ClientEvents.AuctionByItemLoaded(payload.getAuctions()));
    }

    private void handleLoadItemImage(Packet response, Gson gson) {
        ItemImagePayload payload = gson.fromJson(response.getPayload(), ItemImagePayload.class);
        eventBus.publish(new ClientEvents.ItemImageLoaded(payload));
    }

    private void handleItemMutation(Packet response, Gson gson) {
        ResultPayload result = gson.fromJson(response.getPayload(), ResultPayload.class);
        eventBus.publish(new ClientEvents.ItemMutationResult(
                response.getType(), result.getResult(), result.getMessage()));
    }

    private void handleDeleteItem(Packet response, Gson gson) {
        ResultPayload result = gson.fromJson(response.getPayload(), ResultPayload.class);
        eventBus.publish(new ClientEvents.DeleteItemResult(result.getResult(), result.getMessage()));
    }

    private void handleConfirmPayment(Packet response, Gson gson) {
        ResultPayload result = gson.fromJson(response.getPayload(), ResultPayload.class);
        eventBus.publish(new ClientEvents.ConfirmPaymentResult(result.getResult(), result.getMessage()));
    }

    private void handleCancelTransaction(Packet response, Gson gson) {
        ResultPayload result = gson.fromJson(response.getPayload(), ResultPayload.class);
        updateLoggedInUserReputation(result);
        eventBus.publish(new ClientEvents.CancelTransactionResult(result.getResult(), result.getMessage()));
    }

    private void handleRateSeller(Packet response, Gson gson) {
        ResultPayload result = gson.fromJson(response.getPayload(), ResultPayload.class);
        eventBus.publish(new ClientEvents.SellerRatingResult(result.getResult(), result.getMessage()));
    }

    private void handleCancelAuction(Packet response, Gson gson) {
        ResultPayload result = gson.fromJson(response.getPayload(), ResultPayload.class);
        eventBus.publish(new ClientEvents.CancelAuctionResult(result.getResult(), result.getMessage()));
    }

    private void handleLoadProfile(Packet response, Gson gson) {
        ResultPayload result = gson.fromJson(response.getPayload(), ResultPayload.class);
        if (result.getResult()) {
            UserSession.getInstance().setLoggedInUser(toUser(result));
            eventBus.publish(new ClientEvents.UserProfileChanged());
        }
        eventBus.publish(new ClientEvents.CurrentUserLoaded(result));
    }

    private void handleUpdateProfile(Packet response, Gson gson) {
        ResultPayload result = gson.fromJson(response.getPayload(), ResultPayload.class);
        eventBus.publish(new ClientEvents.ProfileUpdateResult(result));
    }

    private void handleReputationUpdated(Packet response, Gson gson) {
        ResultPayload result = gson.fromJson(response.getPayload(), ResultPayload.class);
        updateLoggedInUserReputation(result);
    }

    private void handleChangePassword(Packet response, Gson gson) {
        ResultPayload result = gson.fromJson(response.getPayload(), ResultPayload.class);
        eventBus.publish(new ClientEvents.ChangePasswordResult(result.getResult(), result.getMessage()));
    }

    private void handleLoadUsers(Packet response, Gson gson) {
        UserListResponsePayload payload = gson.fromJson(response.getPayload(), UserListResponsePayload.class);
        eventBus.publish(new ClientEvents.UsersLoaded(payload.getUsers()));
    }

    private void handlePublishAuction(Packet response, Gson gson) {
        ResultPayload result = gson.fromJson(response.getPayload(), ResultPayload.class);
        eventBus.publish(new ClientEvents.PublishAuctionResult(result.getResult(), result.getMessage()));
    }

    private void handleLoadPurchaseHistory(Packet response, Gson gson) {
        PurchaseHistoryResponsePayload payload =
                gson.fromJson(response.getPayload(), PurchaseHistoryResponsePayload.class);
        eventBus.publish(new ClientEvents.PurchaseHistoryLoaded(payload.getHistoryList()));
    }

    private void handlePlaceAutoBid(Packet response, Gson gson) {
        ResultPayload result = gson.fromJson(response.getPayload(), ResultPayload.class);
        eventBus.publish(new ClientEvents.AutoBidPlaced(result.getResult(), result.getMessage()));
    }

    private void handleCheckAutoBid(Packet response, Gson gson) {
        AutoBidPayload payload = gson.fromJson(response.getPayload(), AutoBidPayload.class);
        eventBus.publish(new ClientEvents.AutoBidChecked(payload));
    }

    private void handleCancelAutoBid(Packet response, Gson gson) {
        ResultPayload result = gson.fromJson(response.getPayload(), ResultPayload.class);
        eventBus.publish(new ClientEvents.AutoBidCancelled(result.getResult(), result.getMessage()));
    }

    private void handleLoadDashboard(Packet response, Gson gson) {
        DashboardResponsePayload payload = gson.fromJson(response.getPayload(), DashboardResponsePayload.class);
        eventBus.publish(new ClientEvents.DashboardLoaded(payload));
    }

    private void handleScreenNotify(Packet response, Gson gson) {
        ScreenNotifyPayload payload = gson.fromJson(response.getPayload(), ScreenNotifyPayload.class);
        eventBus.publish(new ClientEvents.ScreenNotified(
                payload.getAuctionId(),
                payload.getHighestPrice(),
                payload.getEventType(),
                payload.getMessage(),
                payload.getStatus()));
    }

    private void handleAuctionSubscribe(Packet response, Gson gson) {
        // Subscription acknowledgement. No UI update needed.
    }

    private void handleDeleteUser(Packet response, Gson gson) {
        ResultPayload result = gson.fromJson(response.getPayload(), ResultPayload.class);
        eventBus.publish(new ClientEvents.UserDeleted(result.getResult(), result.getMessage()));
    }

    private void handleAccountDeleted(Packet response, Gson gson) {
        ResultPayload result = gson.fromJson(response.getPayload(), ResultPayload.class);
        eventBus.publish(new ClientEvents.AccountDeleted(result.getMessage()));
    }

    private void handleSystemLogsResponse(Packet response, Gson gson) {
        SystemLogResponsePayload payload = gson.fromJson(response.getPayload(), SystemLogResponsePayload.class);
        eventBus.publish(new ClientEvents.SystemLogsLoaded(payload.getLogs(), payload.isAppend()));
    }

    private void updateLoggedInUserReputation(ResultPayload result) {
        if (result == null || !result.getResult() || result.getUserId() <= 0) {
            return;
        }

        User currentUser = UserSession.getInstance().getLoggedInUser();
        if (currentUser == null || currentUser.getId() != result.getUserId()) {
            return;
        }

        currentUser.setReputationScore(result.getReputationScore());
        eventBus.publish(new ClientEvents.UserProfileChanged());
    }
}
