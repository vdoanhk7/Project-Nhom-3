package com.nhom3.shared.network.payload;
import java.util.List;

public class BidHistoryResponsePayload {
    public static class SimpleBid {
        public double amount;
        public String timeStr; 
        public String note;
        public String bidderName;

        public SimpleBid(double amount, String timeStr, String note, String bidderName) {
            this.amount = amount;
            this.timeStr = timeStr;
            this.note = note;
            this.bidderName = bidderName;
        }
    }
    private int auctionId;
    private List<SimpleBid> historyList;

    public BidHistoryResponsePayload(int auctionId, List<SimpleBid> historyList) {
        this.auctionId = auctionId;
        this.historyList = historyList;
    }
    public List<SimpleBid> getHistoryList() { return historyList; }
    public int getAuctionId() { return auctionId; }
}