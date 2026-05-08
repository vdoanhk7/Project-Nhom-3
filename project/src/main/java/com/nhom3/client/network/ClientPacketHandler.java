package com.nhom3.client.network;

import com.google.gson.Gson;
import com.nhom3.shared.network.packet.Packet;

public interface ClientPacketHandler {
    void handle(Packet response, Gson gson) throws Exception;
}
