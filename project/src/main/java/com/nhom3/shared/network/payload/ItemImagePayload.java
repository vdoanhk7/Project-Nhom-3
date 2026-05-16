package com.nhom3.shared.network.payload;

public class ItemImagePayload {
    private final int itemId;
    private final String imageBase64;

    public ItemImagePayload(int itemId, String imageBase64) {
        this.itemId = itemId;
        this.imageBase64 = imageBase64;
    }

    public int getItemId() {
        return itemId;
    }

    public String getImageBase64() {
        return imageBase64;
    }
}
