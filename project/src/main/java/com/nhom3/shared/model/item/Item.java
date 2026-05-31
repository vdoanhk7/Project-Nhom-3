package com.nhom3.shared.model.item;

import com.nhom3.shared.model.Entity;

public abstract class Item extends Entity{
    protected String name;
    protected String description;
    protected double startPrice;
    protected double curHighest;
    protected ItemType type;
    protected String imageBase64;
    protected int sellerId;
    protected String sellerName;
    protected double userRatingAverage;
    protected int userRatingCount;

    public Item(int id, String name, double startPrice, ItemType type) {
        super(id);
        this.name = name;
        this.startPrice = startPrice;
        this.type = type;
        // curHighest starting price is startPrice
        this.curHighest = startPrice;

    }

    public void setName(String name){
        this.name = name;
    }
    public String getName(){
        return name;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getDescription() {
        return description;
    }
    public void setStartPrice(double startPrice){
        this.startPrice = startPrice;
    }
    public double getStartPrice(){
        return startPrice;
    }
    public void setCurHighest(double curHighest){
        this.curHighest = curHighest;
    }
    public double getCurHighest(){
        return curHighest;
    }
    public ItemType getType() {
        return type;
    }
    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }
    public String getImageBase64() {
        return imageBase64;
    }
    public int getSellerId() {
        return sellerId;
    }
    public void setSellerId(int sellerId) {
        this.sellerId = sellerId;
    }
    public String getSellerName() {
        return sellerName;
    }
    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }
    public double getUserRatingAverage() {
        return userRatingAverage;
    }
    public int getUserRatingCount() {
        return userRatingCount;
    }
    public void setUserRatingSummary(double userRatingAverage, int userRatingCount) {
        this.userRatingAverage = Math.max(0, Math.min(5, userRatingAverage));
        this.userRatingCount = Math.max(0, userRatingCount);
    }

    protected String formatItemInfo(String categoryName) {
        String owner = sellerName == null || sellerName.isBlank() ? "Chua co thong tin" : sellerName;
        String itemDescription = description == null || description.isBlank() ? "Chua co mo ta" : description;
        return String.format(
                "%s[id=%d, name=%s, description=%s, startPrice=%.0f, currentHighest=%.0f, seller=%s]",
                categoryName,
                id,
                name,
                itemDescription,
                startPrice,
                curHighest,
                owner);
    }

    @Override
    public String printInfo() {
        return formatItemInfo(type == null ? "Item" : type.name());
    }
}
