package com.nhom3.server.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.nhom3.shared.model.auction.Auction;
import com.nhom3.shared.model.auction.StatusOfAuction;
import com.nhom3.shared.model.item.Art;
import com.nhom3.shared.model.user.Bidder;
import com.nhom3.shared.network.payload.AutoBidPayload;

class PacketDispatcherAutoBidPolicyTest {

    @Test
    void shouldKeepCurrentLeaderAutoBidWhenMaxCannotCoverNextBid() {
        Auction auction = runningAuction(10, 500.0, 50.0);
        AutoBidPayload config = new AutoBidPayload(10, 1, 500.0, 50.0);

        assertFalse(PacketDispatcher.shouldCancelAutoBidConfig(config, auction));
    }

    @Test
    void shouldCancelNonLeaderAutoBidWhenMaxCannotCoverNextBid() {
        Auction auction = runningAuction(20, 500.0, 50.0);
        AutoBidPayload config = new AutoBidPayload(10, 1, 500.0, 50.0);

        assertTrue(PacketDispatcher.shouldCancelAutoBidConfig(config, auction));
    }

    @Test
    void shouldCancelAutoBidWhenIncrementIsBelowBidStep() {
        Auction auction = runningAuction(10, 500.0, 50.0);
        AutoBidPayload config = new AutoBidPayload(10, 1, 1_000.0, 25.0);

        assertTrue(PacketDispatcher.shouldCancelAutoBidConfig(config, auction));
    }

    @Test
    void shouldCalculateOpeningAutoBidFromCurrentPriceAndIncrement() {
        Auction auction = runningAuctionWithoutLeader(100.0, 50_000.0);
        AutoBidPayload config = new AutoBidPayload(10, 1, 500_000.0, 50_000.0);

        assertEquals(50_100.0, AuctionHandler.calculateNextAutoBidAmount(auction, config));
    }

    @Test
    void shouldCapNextAutoBidAtMaxAmount() {
        Auction auction = runningAuctionWithoutLeader(100.0, 50_000.0);
        AutoBidPayload config = new AutoBidPayload(10, 1, 75_000.0, 100_000.0);

        assertEquals(75_000.0, AuctionHandler.calculateNextAutoBidAmount(auction, config));
    }

    private Auction runningAuction(int highestBidderId, double currentPrice, double bidStep) {
        Art item = new Art(1, "Auction item", 100.0);
        item.setCurHighest(currentPrice);
        Auction auction = new Auction(1, item, LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1));
        auction.setBidStep(bidStep);
        auction.setStatus(StatusOfAuction.RUNNING);
        auction.setHighestBidder(new Bidder(highestBidderId, null, null));
        return auction;
    }

    private Auction runningAuctionWithoutLeader(double currentPrice, double bidStep) {
        Art item = new Art(1, "Auction item", 100.0);
        item.setCurHighest(currentPrice);
        Auction auction = new Auction(1, item, LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1));
        auction.setBidStep(bidStep);
        auction.setStatus(StatusOfAuction.RUNNING);
        return auction;
    }
}
