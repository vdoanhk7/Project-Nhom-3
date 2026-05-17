package com.nhom3.shared.network.payload;

public class ItemPayload {
    private final int itemId;
    private final int sellerId;
    private final String name;
    private final String description;
    private final String type;
    private final double startPrice;
    private final String imageBase64;

    public ItemPayload(int itemId, int sellerId, String name, String type, double startPrice, String imageBase64) {
        this(itemId, sellerId, name, "", type, startPrice, imageBase64);
    }

    public ItemPayload(int itemId, int sellerId, String name, String description, String type,
            double startPrice, String imageBase64) {
        this.itemId = itemId;
        this.sellerId = sellerId;
        this.name = name;
        this.description = description;
        this.type = type;
        this.startPrice = startPrice;
        this.imageBase64 = imageBase64;
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

    public String getDescription() {
        return description;
    }

    public String getType() {
        return type;
    }

    public double getStartPrice() {
        return startPrice;
    }

    public String getImageBase64() {
        return imageBase64;
    }
}
