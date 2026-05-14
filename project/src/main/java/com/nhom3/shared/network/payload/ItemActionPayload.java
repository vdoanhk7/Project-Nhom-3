package com.nhom3.shared.network.payload;

public class ItemActionPayload {
    private final int itemId;
    private final int sellerId;

    public ItemActionPayload(int itemId, int sellerId) {
        this.itemId = itemId;
        this.sellerId = sellerId;
    }

    public int getItemId() {
        return itemId;
    }

    public int getSellerId() {
        return sellerId;
    }
}
