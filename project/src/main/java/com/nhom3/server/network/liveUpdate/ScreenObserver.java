package com.nhom3.server.network.liveUpdate;

import com.nhom3.server.network.ClientHandler;
import com.nhom3.shared.network.packet.*;
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
}
