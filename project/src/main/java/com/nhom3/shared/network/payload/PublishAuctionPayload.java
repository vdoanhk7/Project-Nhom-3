package com.nhom3.shared.network.payload;

public class PublishAuctionPayload {
    private int itemId;
    private String startTime; // Dùng String để né lỗi Gson
    private String endTime;
    private double bidStep;
    private int sellerId;

    public PublishAuctionPayload(int itemId, String startTime, String endTime) {
        this(itemId, startTime, endTime, com.nhom3.shared.model.auction.Auction.DEFAULT_BID_STEP, -1);
    }

    public PublishAuctionPayload(int itemId, String startTime, String endTime, double bidStep) {
        this(itemId, startTime, endTime, bidStep, -1);
    }

    public PublishAuctionPayload(int itemId, String startTime, String endTime, double bidStep, int sellerId) {
        this.itemId = itemId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.bidStep = bidStep;
        this.sellerId = sellerId;
    }
    public int getItemId() { return itemId; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public double getBidStep() { return bidStep; }
    public int getSellerId() { return sellerId; }
}
