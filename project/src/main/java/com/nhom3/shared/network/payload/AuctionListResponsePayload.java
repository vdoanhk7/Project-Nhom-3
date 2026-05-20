package com.nhom3.shared.network.payload;

import java.util.List;

public class AuctionListResponsePayload {
    private final List<AuctionDTO> auctions;

    public AuctionListResponsePayload(List<AuctionDTO> auctions) {
        this.auctions = auctions;
    }

    public List<AuctionDTO> getAuctions() {
        return auctions;
    }

    public static class AuctionDTO {
        public final int auctionId;
        public final int itemId;
        public final String itemName;
        public final String itemDescription;
        public final String itemType;
        public final double startPrice;
        public final double curHighest;
        public final double bidStep;
        public final String startTime;
        public final String endTime;
        public final String status;
        public final int highestBidderId;
        public final String imageBase64;
        public final int sellerId;
        public final String sellerName;
        public final double sellerRatingAverage;
        public final int sellerRatingCount;

        public AuctionDTO(int auctionId, int itemId, String itemName, String itemType,
                double startPrice, double curHighest, String startTime, String endTime,
                String status, int highestBidderId) {
            this(auctionId, itemId, itemName, "", itemType, startPrice, curHighest,
                    com.nhom3.shared.model.auction.Auction.DEFAULT_BID_STEP,
                    startTime, endTime, status, highestBidderId, null, -1, "", 0, 0);
        }

        public AuctionDTO(int auctionId, int itemId, String itemName, String itemType,
                double startPrice, double curHighest, String startTime, String endTime,
                String status, int highestBidderId, String imageBase64) {
            this(auctionId, itemId, itemName, "", itemType, startPrice, curHighest,
                    com.nhom3.shared.model.auction.Auction.DEFAULT_BID_STEP,
                    startTime, endTime, status, highestBidderId, imageBase64, -1, "", 0, 0);
        }

        public AuctionDTO(int auctionId, int itemId, String itemName, String itemType,
                double startPrice, double curHighest, double bidStep, String startTime, String endTime,
                String status, int highestBidderId, String imageBase64, String sellerName) {
            this(auctionId, itemId, itemName, "", itemType, startPrice, curHighest, bidStep,
                    startTime, endTime, status, highestBidderId, imageBase64, -1, sellerName, 0, 0);
        }

        public AuctionDTO(int auctionId, int itemId, String itemName, String itemDescription, String itemType,
                double startPrice, double curHighest, double bidStep, String startTime, String endTime,
                String status, int highestBidderId, String imageBase64, int sellerId, String sellerName,
                double sellerRatingAverage, int sellerRatingCount) {
            this.auctionId = auctionId;
            this.itemId = itemId;
            this.itemName = itemName;
            this.itemDescription = itemDescription;
            this.itemType = itemType;
            this.startPrice = startPrice;
            this.curHighest = curHighest;
            this.bidStep = bidStep;
            this.startTime = startTime;
            this.endTime = endTime;
            this.status = status;
            this.highestBidderId = highestBidderId;
            this.imageBase64 = imageBase64;
            this.sellerId = sellerId;
            this.sellerName = sellerName;
            this.sellerRatingAverage = Math.max(0, Math.min(5, sellerRatingAverage));
            this.sellerRatingCount = Math.max(0, sellerRatingCount);
        }

        public AuctionDTO(int auctionId, int itemId, String itemName, String itemDescription, String itemType,
                double startPrice, double curHighest, double bidStep, String startTime, String endTime,
                String status, int highestBidderId, String imageBase64, String sellerName) {
            this(auctionId, itemId, itemName, itemDescription, itemType, startPrice, curHighest, bidStep,
                    startTime, endTime, status, highestBidderId, imageBase64, -1, sellerName, 0, 0);
        }
    }
}
