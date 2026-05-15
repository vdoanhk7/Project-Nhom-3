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
    LOAD_ACTIVE_AUCTIONS("load active auctions"),
    LOAD_AUCTION_BY_ITEM("load auction by item"),
    SAVE_ITEM("save item"),
    UPDATE_ITEM("update item"),
    DELETE_ITEM("delete item"),
    CONFIRM_PAYMENT("confirm payment"),
    CANCEL_AUCTION("cancel auction"),
    UPDATE_PROFILE("update profile"),
    CHANGE_PASSWORD("change password"),
    LOAD_USERS("load users"),
    PUBLISH_AUCTION("publish auction"),
    LOAD_BID_HISTORY("load bid history"),
    LOAD_PURCHASE_HISTORY("load purchase history"),
    SCREEN_NOTIFY("screen notify"),
    AUCTION_SUBSCRIBE("auction subscribe"),
    LOAD_DASHBOARD("load dashboard"),
    DELETE_USER("delete user"),
    GET_SYSTEM_LOGS("get system logs"),
    SYSTEM_LOGS_RESPONSE("system logs response");


    private String message;

    PacketType(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
