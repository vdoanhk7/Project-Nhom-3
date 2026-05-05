package com.nhom3.shared.network.payload;

public class PublishAuctionPayload {
    private int itemId;
    private String startTime; // Dùng String để né lỗi Gson
    private String endTime;

    public PublishAuctionPayload(int itemId, String startTime, String endTime) {
        this.itemId = itemId;
        this.startTime = startTime;
        this.endTime = endTime;
    }
    public int getItemId() { return itemId; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
}