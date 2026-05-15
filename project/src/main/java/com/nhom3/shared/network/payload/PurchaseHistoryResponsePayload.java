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
        public double bidStep;
        public String startTimeStr;
        public String endTimeStr;
        public String imageBase64;

        public HistoryDTO(int auctionId, int itemId, String itemName, double myBidAmount, String myBidTimeStr, String status, int topBidderId,
                          String itemType, double startPrice, double curHighest, String startTimeStr, String endTimeStr) {
            this(auctionId, itemId, itemName, myBidAmount, myBidTimeStr, status, topBidderId,
                    itemType, startPrice, curHighest,
                    com.nhom3.shared.model.auction.Auction.DEFAULT_BID_STEP,
                    startTimeStr, endTimeStr, null);
        }

        public HistoryDTO(int auctionId, int itemId, String itemName, double myBidAmount, String myBidTimeStr, String status, int topBidderId,
                          String itemType, double startPrice, double curHighest, double bidStep, String startTimeStr, String endTimeStr, String imageBase64) {
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
            this.bidStep = bidStep;
            this.startTimeStr = startTimeStr;
            this.endTimeStr = endTimeStr;
            this.imageBase64 = imageBase64;
        }
    }

    private List<HistoryDTO> historyList;
    public PurchaseHistoryResponsePayload(List<HistoryDTO> historyList) { this.historyList = historyList; }
    public List<HistoryDTO> getHistoryList() { return historyList; }
}
