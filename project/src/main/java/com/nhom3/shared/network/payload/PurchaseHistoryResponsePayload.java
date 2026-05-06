package com.nhom3.shared.network.payload;
import java.util.List;

public class PurchaseHistoryResponsePayload {
    
    public static class HistoryDTO {
        public int auctionId;
        public int itemId;
        public String itemName;
        public double myBidAmount;
        public String myBidTimeStr; 
        public String status;
        public int topBidderId;
    
        public String itemType;
        public double startPrice;
        public double curHighest;
        public String startTimeStr;
        public String endTimeStr;

        public HistoryDTO(int auctionId, int itemId, String itemName, double myBidAmount, String myBidTimeStr, String status, int topBidderId,
                          String itemType, double startPrice, double curHighest, String startTimeStr, String endTimeStr) {
            this.auctionId = auctionId;
            this.itemId = itemId;
            this.itemName = itemName;
            this.myBidAmount = myBidAmount;
            this.myBidTimeStr = myBidTimeStr;
            this.status = status;
            this.topBidderId = topBidderId;
            this.itemType = itemType;
            this.startPrice = startPrice;
            this.curHighest = curHighest;
            this.startTimeStr = startTimeStr;
            this.endTimeStr = endTimeStr;
        }
    }

    private List<HistoryDTO> historyList;
    public PurchaseHistoryResponsePayload(List<HistoryDTO> historyList) { this.historyList = historyList; }
    public List<HistoryDTO> getHistoryList() { return historyList; }
}