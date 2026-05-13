package com.nhom3.shared.network.packet;

public enum PacketType {
    LOGIN("login"),
    REGISTER("register"),
    PLACE_BID("placebid"),
    RESULT("result"),   
    PLACE_AUTO_BID("place auto bid"),
    CHECK_AUTO_BID("check auto bid"), 
    CANCEL_AUTO_BID("cancel auto bid"),
    LOAD_SELLER_ITEMS("load seller items"),
    PUBLISH_AUCTION("publish auction"),
    LOAD_BID_HISTORY("load bid history"),
    LOAD_PURCHASE_HISTORY("load purchase history"),
    SCREEN_NOTIFY("screen notify"),
    AUCTION_SUBSCRIBE("auction subscribe"),
    LOAD_DASHBOARD("load dashboard");


    private String message;

    PacketType(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}