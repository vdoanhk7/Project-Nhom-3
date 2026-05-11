package com.nhom3.server.network;

import com.google.gson.Gson;
import com.nhom3.shared.network.packet.Packet;

public interface PacketHandler {
    Packet handle(Packet request, Gson gson) throws Exception;
}
