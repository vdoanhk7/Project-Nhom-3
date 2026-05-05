package com.nhom3.shared.network.packet;

public class Packet {
    private PacketType type;
    private Object payload;

    public Packet(PacketType type, Object payload) {
        this.type = type;
        this.payload = payload;
    }

    // Getters and setters
    public PacketType getType() {
        return type;
    }

    public Object getPayload() {
        return payload;
    }

    public void setType(PacketType type) {
        this.type = type;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }
}
