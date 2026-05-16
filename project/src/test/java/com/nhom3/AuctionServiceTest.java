package com.nhom3;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.nhom3.server.dao.AuctionDAO;
import com.nhom3.server.service.AuctionService;
import com.nhom3.shared.model.auction.Auction;
import com.nhom3.shared.model.auction.BidTransaction;
import com.nhom3.shared.model.auction.StatusOfAuction;
import com.nhom3.shared.model.item.Art;
import com.nhom3.shared.model.item.Item;
import com.nhom3.shared.model.user.Bidder;

class AuctionServiceTest {

    @Mock
    private AuctionDAO auctionDAO;

    private AuctionService auctionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        auctionService = new AuctionService(auctionDAO);
    }

    @Test
    void testCreateAuction_NullAuction_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> auctionService.createAuction(null));
    }

    @Test
    void testCreateAuction_NullItem_ThrowsException() {
        Auction auction = new Auction(1, null, LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        assertThrows(IllegalArgumentException.class, () -> auctionService.createAuction(auction));
    }

    @Test
    void testCreateAuction_InvalidItemId_ThrowsException() {
        Item item = mock(Item.class);
        when(item.getId()).thenReturn(0);
        Auction auction = new Auction(1, item, LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        assertThrows(IllegalArgumentException.class, () -> auctionService.createAuction(auction));
    }

    @Test
    void testCreateAuction_EndTimeBeforeStartTime_ThrowsException() {
        Item item = mock(Item.class);
        when(item.getId()).thenReturn(1);
        Auction auction = new Auction(1, item, LocalDateTime.now().plusHours(1), LocalDateTime.now());

        assertThrows(IllegalArgumentException.class, () -> auctionService.createAuction(auction));
    }

    @Test
    void testCreateAuction_InvalidBidStep_ThrowsException() {
        Item item = mock(Item.class);
        when(item.getId()).thenReturn(1);
        Auction auction = new Auction(1, item, LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        auction.setBidStep(0);

        assertThrows(IllegalArgumentException.class, () -> auctionService.createAuction(auction));
    }

    @Test
    void testCreateAuction_AlreadyHasRunningAuction_ThrowsException() {
        Item item = mock(Item.class);
        when(item.getId()).thenReturn(1);
        Auction auction = new Auction(1, item, LocalDateTime.now(), LocalDateTime.now().plusHours(1));

        Auction existingAuction = new Auction(2, item, LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        existingAuction.setStatus(StatusOfAuction.RUNNING);

        when(auctionDAO.getAuctionByItemId(1)).thenReturn(existingAuction);

        assertThrows(IllegalStateException.class, () -> auctionService.createAuction(auction));
    }

    @Test
    void testCreateAuction_Success_FutureTime() {
        Item item = mock(Item.class);
        when(item.getId()).thenReturn(1);
        Auction auction = new Auction(1, item, LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(2));

        when(auctionDAO.getAuctionByItemId(1)).thenReturn(null);
        when(auctionDAO.createAuction(auction)).thenReturn(true);

        boolean result = auctionService.createAuction(auction);

        assertTrue(result);
        assertEquals(StatusOfAuction.OPEN, auction.getStatus());
        verify(auctionDAO).createAuction(auction);
    }

    @Test
    void testStartAuction_Success() {
        Item item = mock(Item.class);
        Auction auction = new Auction(1, item, LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        auction.setStatus(StatusOfAuction.OPEN);

        when(auctionDAO.startAuction(1)).thenReturn(true);

        boolean result = auctionService.startAuction(auction);
        assertTrue(result);
    }

    @Test
    void testStartAuction_Fail_AlreadyRunning() {
        Item item = mock(Item.class);
        Auction auction = new Auction(1, item, LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        auction.setStatus(StatusOfAuction.RUNNING);

        boolean result = auctionService.startAuction(auction);
        assertFalse(result);
        verify(auctionDAO, never()).startAuction(anyInt());
    }

    @Test
    void testEndAuction_Success() {
        Item item = mock(Item.class);
        Auction auction = new Auction(1, item, LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        auction.setStatus(StatusOfAuction.RUNNING);

        when(auctionDAO.endAuction(1)).thenReturn(true);

        boolean result = auctionService.endAuction(auction);
        assertTrue(result);
    }

    @Test
    void testCancelAuction_Success() {
        when(auctionDAO.cancelAuction(1)).thenReturn(true);

        boolean result = auctionService.cancelAuction(1);
        assertTrue(result);
    }

    @Test
    void testCancelAuction_Fail() {
        when(auctionDAO.cancelAuction(1)).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> auctionService.cancelAuction(1));
    }

    @Test
    void testPlaceBid_Success() {
        Item item = mock(Item.class);
        when(item.getId()).thenReturn(1);
        when(item.getCurHighest()).thenReturn(100.0);

        Auction auction = new Auction(1, item, LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1));
        auction.setBidStep(50.0);
        auction.setStatus(StatusOfAuction.RUNNING);
        auction.setHighestBidder(new Bidder(1, null, null)); // highest bidder is 1

        Bidder bidder = new Bidder(2, null, null); // bidder 2 places bid
        BidTransaction bid = new BidTransaction(1, bidder, 150.0, LocalDateTime.now(), "Test");

        when(auctionDAO.updateHighestBid(1, 2, 150.0)).thenReturn(true);
        when(auctionDAO.getEndTime(1)).thenReturn(auction.getEndTime());

        boolean result = auctionService.placeBid(auction, bid);

        assertTrue(result);
        verify(item).setCurHighest(150.0);
        assertEquals(bidder, auction.getHighestBidder());
        verify(auctionDAO).saveBidTransaction(bid, 1);
    }

    @Test
    void testPlaceBid_BelowBidStep_ThrowsException() {
        Item item = mock(Item.class);
        when(item.getId()).thenReturn(1);
        when(item.getCurHighest()).thenReturn(100.0);

        Auction auction = new Auction(1, item, LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1));
        auction.setBidStep(50.0);
        auction.setStatus(StatusOfAuction.RUNNING);
        auction.setHighestBidder(new Bidder(1, null, null));

        Bidder bidder = new Bidder(2, null, null);
        BidTransaction bid = new BidTransaction(1, bidder, 149.0, LocalDateTime.now(), "Test");

        assertThrows(IllegalStateException.class, () -> auctionService.placeBid(auction, bid));
        verify(auctionDAO, never()).updateHighestBid(anyInt(), anyInt(), anyDouble());
    }

    @Test
    void testPlaceBid_SameBidder_ThrowsException() {
        Item item = mock(Item.class);
        when(item.getId()).thenReturn(1);

        Auction auction = new Auction(1, item, LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1));
        auction.setStatus(StatusOfAuction.RUNNING);
        auction.setHighestBidder(new Bidder(1, null, null)); // highest bidder is 1

        Bidder bidder = new Bidder(1, null, null); // bidder 1 places bid again
        BidTransaction bid = new BidTransaction(1, bidder, 150.0, LocalDateTime.now(), "Test");

        assertThrows(IllegalStateException.class, () -> auctionService.placeBid(auction, bid));
    }

    @Test
    void testPlaceBid_CancelledAuction_ThrowsExceptionAndDoesNotUpdateDb() {
        Auction auction = runningAuctionWithRealItem();
        auction.setStatus(StatusOfAuction.CANCELLED);
        BidTransaction bid = bidFromUser(2, 200.0);

        assertThrows(IllegalStateException.class, () -> auctionService.placeBid(auction, bid));
        verify(auctionDAO, never()).updateHighestBid(anyInt(), anyInt(), anyDouble());
        verify(auctionDAO, never()).saveBidTransaction(any(), anyInt());
    }

    @Test
    void testPlaceBid_BeforeStartTime_ThrowsExceptionAndDoesNotUpdateDb() {
        Auction auction = new Auction(
                1,
                new Art(1, "Future item", 100.0),
                LocalDateTime.now().plusMinutes(10),
                LocalDateTime.now().plusHours(1));
        auction.setBidStep(50.0);
        auction.setStatus(StatusOfAuction.OPEN);
        BidTransaction bid = bidFromUser(2, 200.0);

        assertThrows(IllegalStateException.class, () -> auctionService.placeBid(auction, bid));
        verify(auctionDAO, never()).updateHighestBid(anyInt(), anyInt(), anyDouble());
        verify(auctionDAO, never()).saveBidTransaction(any(), anyInt());
    }

    @Test
    void testPlaceBid_AfterEndTime_ThrowsExceptionAndDoesNotUpdateDb() {
        Auction auction = new Auction(
                1,
                new Art(1, "Closed item", 100.0),
                LocalDateTime.now().minusHours(2),
                LocalDateTime.now().minusMinutes(1));
        auction.setBidStep(50.0);
        auction.setStatus(StatusOfAuction.RUNNING);
        BidTransaction bid = bidFromUser(2, 200.0);

        assertThrows(IllegalStateException.class, () -> auctionService.placeBid(auction, bid));
        verify(auctionDAO, never()).updateHighestBid(anyInt(), anyInt(), anyDouble());
        verify(auctionDAO, never()).saveBidTransaction(any(), anyInt());
    }

    @Test
    void testPlaceBid_WhenDaoRejectsBid_DoesNotSaveTransactionOrMutateAuction() {
        Auction auction = runningAuctionWithRealItem();
        Bidder previousLeader = new Bidder(1, null, null);
        auction.setHighestBidder(previousLeader);
        BidTransaction bid = bidFromUser(2, 200.0);

        when(auctionDAO.updateHighestBid(1, 2, 200.0)).thenReturn(false);

        boolean result = auctionService.placeBid(auction, bid);

        assertFalse(result);
        assertEquals(100.0, auction.getItem().getCurHighest());
        assertEquals(previousLeader, auction.getHighestBidder());
        verify(auctionDAO, never()).saveBidTransaction(any(), anyInt());
        verify(auctionDAO, never()).extendAuctionTime(anyInt(), anyInt());
    }

    @Test
    void testPlaceBid_NearEndTime_ExtendsAuctionForAntiSniping() {
        Auction auction = runningAuctionWithRealItem();
        BidTransaction bid = bidFromUser(2, 200.0);
        LocalDateTime endTimeWithinThreshold = LocalDateTime.now().plusSeconds(20);

        when(auctionDAO.updateHighestBid(1, 2, 200.0)).thenReturn(true);
        when(auctionDAO.getEndTime(1)).thenReturn(endTimeWithinThreshold);

        boolean result = auctionService.placeBid(auction, bid);

        assertTrue(result);
        verify(auctionDAO).saveBidTransaction(bid, 1);
        verify(auctionDAO).extendAuctionTime(1, 2);
    }

    @Test
    void testPlaceBid_NotNearEndTime_DoesNotExtendAuction() {
        Auction auction = runningAuctionWithRealItem();
        BidTransaction bid = bidFromUser(2, 200.0);
        LocalDateTime endTimeOutsideThreshold = LocalDateTime.now().plusMinutes(10);

        when(auctionDAO.updateHighestBid(1, 2, 200.0)).thenReturn(true);
        when(auctionDAO.getEndTime(1)).thenReturn(endTimeOutsideThreshold);

        boolean result = auctionService.placeBid(auction, bid);

        assertTrue(result);
        verify(auctionDAO).saveBidTransaction(bid, 1);
        verify(auctionDAO, never()).extendAuctionTime(anyInt(), anyInt());
    }

    private Auction runningAuctionWithRealItem() {
        Art item = new Art(1, "Auction item", 100.0);
        Auction auction = new Auction(1, item, LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1));
        auction.setBidStep(50.0);
        auction.setStatus(StatusOfAuction.RUNNING);
        auction.setHighestBidder(new Bidder(1, null, null));
        return auction;
    }

    private BidTransaction bidFromUser(int bidderId, double amount) {
        return new BidTransaction(1, new Bidder(bidderId, null, null), amount, LocalDateTime.now(), "Test");
    }
}
