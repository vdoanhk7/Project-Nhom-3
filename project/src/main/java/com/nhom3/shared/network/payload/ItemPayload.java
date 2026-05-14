package com.nhom3.shared.network.payload;

public class ItemPayload {
    private final int itemId;
    private final int sellerId;
    private final String name;
    private final String type;
    private final double startPrice;

    public ItemPayload(int itemId, int sellerId, String name, String type, double startPrice) {
        this.itemId = itemId;
        this.sellerId = sellerId;
        this.name = name;
        this.type = type;
        this.startPrice = startPrice;
    }

    public int getItemId() {
        return itemId;
    }

    public int getSellerId() {
        return sellerId;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public double getStartPrice() {
        return startPrice;
    }
}
