package com.nhom3.shared.network.payload;

public class SellerRatingPayload {
    private final int auctionId;
    private final int buyerId;
    private final int stars;

    public SellerRatingPayload(int auctionId, int buyerId, int stars) {
        this.auctionId = auctionId;
        this.buyerId = buyerId;
        this.stars = stars;
    }

    public int getAuctionId() {
        return auctionId;
    }

    public int getBuyerId() {
        return buyerId;
    }

    public int getStars() {
        return stars;
    }
}
