package com.nhom3.client.event;

import com.nhom3.shared.model.user.User;
import com.nhom3.shared.network.packet.PacketType;
import com.nhom3.shared.network.payload.AuctionListResponsePayload;
import com.nhom3.shared.network.payload.AutoBidPayload;
import com.nhom3.shared.network.payload.BidHistoryResponsePayload;
import com.nhom3.shared.network.payload.DashboardResponsePayload;
import com.nhom3.shared.network.payload.ItemImagePayload;
import com.nhom3.shared.network.payload.PurchaseHistoryResponsePayload;
import com.nhom3.shared.network.payload.ResultPayload;
import com.nhom3.shared.network.payload.SellerItemsResponsePayload;
import com.nhom3.shared.network.payload.UserListResponsePayload;
import java.util.List;

public final class ClientEvents {
    private ClientEvents() {
    }

    public enum Route {
        DASHBOARD,
        MARKET,
        PURCHASE_HISTORY,
        MANAGE_ITEM,
        ADMIN_PANEL,
        PROFILE
    }

    public record NavigationRequested(Route route) {
    }

    public record UserProfileChanged() {
    }

    public record SellerItemsChanged() {
    }

    public record LoginResult(boolean success, User user) {
    }

    public record SignupResult(boolean success, String message) {
    }

    public record BidResult(boolean success, String message) {
    }

    public record BidHistoryLoaded(int auctionId, List<BidHistoryResponsePayload.SimpleBid> historyList) {
    }

    public record SellerItemsLoaded(List<SellerItemsResponsePayload.SellerItemDTO> items) {
    }

    public record ActiveAuctionsLoaded(List<AuctionListResponsePayload.AuctionDTO> auctions) {
    }

    public record AllAuctionsLoaded(List<AuctionListResponsePayload.AuctionDTO> auctions) {
    }

    public record AuctionByItemLoaded(List<AuctionListResponsePayload.AuctionDTO> auctions) {
    }

    public record ItemImageLoaded(ItemImagePayload payload) {
    }

    public record ItemMutationResult(PacketType packetType, boolean success, String message) {
    }

    public record DeleteItemResult(boolean success, String message) {
    }

    public record ConfirmPaymentResult(boolean success, String message) {
    }

    public record CancelAuctionResult(boolean success, String message) {
    }

    public record ProfileUpdateResult(ResultPayload result) {
    }

    public record ChangePasswordResult(boolean success, String message) {
    }

    public record UsersLoaded(List<UserListResponsePayload.UserDTO> users) {
    }

    public record PublishAuctionResult(boolean success, String message) {
    }

    public record PurchaseHistoryLoaded(List<PurchaseHistoryResponsePayload.HistoryDTO> historyList) {
    }

    public record AutoBidPlaced(boolean success, String message) {
    }

    public record AutoBidChecked(AutoBidPayload config) {
    }

    public record AutoBidCancelled(boolean success, String message) {
    }

    public record DashboardLoaded(DashboardResponsePayload data) {
    }

    public record ScreenNotified(int auctionId, double highestPrice) {
    }

    public record UserDeleted(boolean success, String message) {
    }

    public record AccountDeleted(String message) {
    }

    public record SystemLogsLoaded(String logs, boolean isAppend) {
    }
}
