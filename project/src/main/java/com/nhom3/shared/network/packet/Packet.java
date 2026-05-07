package com.nhom3.shared.network.packet;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

public class Packet {
    private PacketType type;
    private JsonElement payload;
    
    private static final Gson gson = new Gson();

    public Packet(PacketType type, JsonElement payload) {
        this.type = type;
        this.payload = payload;
    }

    public Packet(PacketType type, Object payloadObject) {
        this.type = type;
        if (payloadObject != null) {
            this.payload = gson.toJsonTree(payloadObject);
        } else {
            this.payload = null;
        }
    }

    // Getters and setters
    public PacketType getType() {
        return type;
    }

    public JsonElement getPayload() {
        return payload;
    }

    public void setType(PacketType type) {
        this.type = type;
    }

    public void setPayload(JsonElement payload) {
        this.payload = payload;
    }
}