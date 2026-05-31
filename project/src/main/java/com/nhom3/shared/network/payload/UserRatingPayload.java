package com.nhom3.shared.network.payload;

public class UserRatingPayload {
    private final int auctionId;
    private final int buyerId;
    private final int stars;

    public UserRatingPayload(int auctionId, int buyerId, int stars) {
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
