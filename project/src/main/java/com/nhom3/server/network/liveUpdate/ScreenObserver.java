package com.nhom3.server.network.liveUpdate;

import com.nhom3.server.network.ClientHandler;
import com.nhom3.shared.model.auction.Auction;
import com.nhom3.shared.network.packet.*;
import com.nhom3.shared.network.payload.BidHistoryResponsePayload;
import com.nhom3.shared.network.payload.ScreenNotifyPayload;

import java.io.IOException;

public class ScreenObserver extends AuctionObserver {
    public ScreenObserver(ClientHandler client) {
        super(client);
    }

    @Override
    public void update(int auctionId, double amount) throws IOException {
        ScreenNotifyPayload UpdatePrice = new ScreenNotifyPayload(auctionId, amount);
        Packet ScreenNotifyPacket = new Packet(PacketType.SCREEN_NOTIFY, UpdatePrice);
        client.send(ScreenNotifyPacket);
    }

    public void notifySnapshot(
            Auction auction,
            String eventType,
            String message,
            BidHistoryResponsePayload.SimpleBid latestBid) throws IOException {
        int auctionId = auction != null ? auction.getId() : -1;
        double highestPrice = auction != null && auction.getItem() != null ? auction.getItem().getCurHighest() : 0;
        int highestBidderId = auction != null ? auction.getHighestBidderId() : -1;
        String status = auction != null && auction.getStatus() != null ? auction.getStatus().name() : null;
        String endTime = auction != null && auction.getEndTime() != null ? auction.getEndTime().toString() : null;

        ScreenNotifyPayload payload = new ScreenNotifyPayload(
                auctionId,
                highestPrice,
                highestBidderId,
                eventType,
                message,
                status,
                endTime,
                latestBid);
        Packet packet = new Packet(PacketType.SCREEN_NOTIFY, payload);
        client.send(packet);
    }

    public void notifyCancelled(int auctionId, String message) throws IOException {
        ScreenNotifyPayload payload = new ScreenNotifyPayload(
                auctionId, 0, "AUCTION_CANCELLED", message, "CANCELLED");
        Packet packet = new Packet(PacketType.SCREEN_NOTIFY, payload);
        client.send(packet);
    }

    public void notifyDealCancelled(int auctionId, String message) throws IOException {
        ScreenNotifyPayload payload = new ScreenNotifyPayload(
                auctionId, 0, "DEAL_CANCELLED", message, "DEAL_CANCELLED");
        Packet packet = new Packet(PacketType.SCREEN_NOTIFY, payload);
        client.send(packet);
    }
}
