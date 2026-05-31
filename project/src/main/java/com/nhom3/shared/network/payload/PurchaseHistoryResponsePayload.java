package com.nhom3.shared.network.payload;
import java.util.List;

public class PurchaseHistoryResponsePayload {
    
    public static class HistoryDTO {
        public int auctionId;
        public int itemId;
        public String itemName;
        public String itemDescription;
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
        public int sellerId;
        public String sellerName;
        public double userRatingAverage;
        public int userRatingCount;
        public int myUserRating;

        public HistoryDTO(int auctionId, int itemId, String itemName, double myBidAmount, String myBidTimeStr, String status, int topBidderId,
                          String itemType, double startPrice, double curHighest, String startTimeStr, String endTimeStr) {
            this(auctionId, itemId, itemName, "", myBidAmount, myBidTimeStr, status, topBidderId,
                    itemType, startPrice, curHighest,
                    com.nhom3.shared.model.auction.Auction.DEFAULT_BID_STEP,
                    startTimeStr, endTimeStr, null, -1, "", 0, 0, -1);
        }

        public HistoryDTO(int auctionId, int itemId, String itemName, double myBidAmount, String myBidTimeStr, String status, int topBidderId,
                           String itemType, double startPrice, double curHighest, double bidStep, String startTimeStr, String endTimeStr, String imageBase64) {
            this(auctionId, itemId, itemName, "", myBidAmount, myBidTimeStr, status, topBidderId,
                    itemType, startPrice, curHighest, bidStep, startTimeStr, endTimeStr, imageBase64,
                    -1, "", 0, 0, -1);
        }

        public HistoryDTO(int auctionId, int itemId, String itemName, String itemDescription, double myBidAmount,
                           String myBidTimeStr, String status, int topBidderId,
                           String itemType, double startPrice, double curHighest, double bidStep,
                           String startTimeStr, String endTimeStr, String imageBase64) {
            this(auctionId, itemId, itemName, itemDescription, myBidAmount, myBidTimeStr, status, topBidderId,
                    itemType, startPrice, curHighest, bidStep, startTimeStr, endTimeStr, imageBase64,
                    -1, "", 0, 0, -1);
        }

        public HistoryDTO(int auctionId, int itemId, String itemName, String itemDescription, double myBidAmount,
                           String myBidTimeStr, String status, int topBidderId,
                           String itemType, double startPrice, double curHighest, double bidStep,
                           String startTimeStr, String endTimeStr, String imageBase64,
                           int sellerId, String sellerName, double userRatingAverage,
                           int userRatingCount, int myUserRating) {
            this.auctionId = auctionId;
            this.itemId = itemId;
            this.itemName = itemName;
            this.itemDescription = itemDescription;
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
            this.sellerId = sellerId;
            this.sellerName = sellerName;
            this.userRatingAverage = Math.max(0, Math.min(5, userRatingAverage));
            this.userRatingCount = Math.max(0, userRatingCount);
            this.myUserRating = myUserRating < 0 ? -1 : Math.min(5, myUserRating);
        }
    }

    private List<HistoryDTO> historyList;
    public PurchaseHistoryResponsePayload(List<HistoryDTO> historyList) { this.historyList = historyList; }
    public List<HistoryDTO> getHistoryList() { return historyList; }
}
