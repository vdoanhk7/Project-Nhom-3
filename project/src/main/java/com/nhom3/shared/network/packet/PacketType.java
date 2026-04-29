package com.nhom3.shared.network.packet;

public enum PacketType {
    LOGIN("login"),
    REGISTER("register"),
    PLACE_BID("placebid"),
    RESULT("result"),   
    PLACE_AUTO_BID("place auto bid");


    private String message;

    PacketType(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}