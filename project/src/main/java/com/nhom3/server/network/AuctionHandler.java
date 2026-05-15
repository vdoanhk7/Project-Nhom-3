package com.nhom3.server.network;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nhom3.server.dao.AuctionDAOImpl;
import com.nhom3.server.network.liveUpdate.Announcer;
import com.nhom3.server.service.AuctionService;
import com.nhom3.shared.model.auction.Auction;
import com.nhom3.shared.model.auction.BidTransaction;
import com.nhom3.shared.model.user.Bidder;
import com.nhom3.shared.network.payload.AutoBidPayload;
import com.nhom3.shared.network.payload.BidPayload;
import com.nhom3.shared.network.payload.ResultPayload;

public class AuctionHandler {
    private static final Logger logger = LoggerFactory.getLogger(AuctionHandler.class);
    private static final int AUCTION_SHARDS = 10;
    private static AuctionHandler instance;

    private final AuctionDAOImpl dao;
    private final AuctionService auctionService;
    private final Announcer announcer;
    private final ExecutorService[] auctionThreads;

    public synchronized static AuctionHandler getInstance() {
        if (instance == null) {
            instance = new AuctionHandler();
        }
        return instance;
    }

    private AuctionHandler() {
        auctionService = new AuctionService();
        dao = new AuctionDAOImpl();
        announcer = Announcer.getInstance();
        auctionThreads = new ExecutorService[AUCTION_SHARDS];
        for (int i = 0; i < AUCTION_SHARDS; i++) {
            auctionThreads[i] = Executors.newSingleThreadExecutor();
        }
    }

    // Kill thread nếu server tắt

    public void shutdown() {
        for (int i = 0; i < AUCTION_SHARDS; i++) {
            auctionThreads[i].shutdownNow();
        }
    }

    // Handle Bid/AutoBid
    public ResultPayload handleBid(BidPayload bidData) {
        Callable<ResultPayload> placeBidTask;
        placeBidTask = () -> {
            try {
                Auction currentAuction = dao.getAuctionById(bidData.getAuctionId());
                if (currentAuction == null)
                    throw new IllegalStateException("Không tìm thấy phiên đấu giá này!");

                Bidder bidder = new Bidder(bidData.getUserId(), null, null);
                BidTransaction newBid = new BidTransaction(0, bidder, bidData.getAmount(), LocalDateTime.now(),
                        "Đặt giá qua mạng");
                boolean isBidSuccess = auctionService.placeBid(currentAuction, newBid);

                ResultPayload bidResultPayload = new ResultPayload(isBidSuccess,
                        isBidSuccess ? "Đặt giá thành công" : "Có người đã trả giá cao hơn, vui lòng thử lại!", -1, "",
                        "", "", "", "");
                if (isBidSuccess) {
                    announcer.notify(bidData.getAuctionId(), bidData.getAmount());
                }
                return bidResultPayload;
            } catch (IllegalStateException e) {
                ResultPayload bidResultPayload = new ResultPayload(false, e.getMessage(), -1, "", "", "", "", "");
                return bidResultPayload;
            } catch (Exception e) {
                logger.error("Lỗi đặt giá", e);
                ResultPayload bidResultPayload = new ResultPayload(false, "Lỗi hệ thống máy chủ!", -1, "", "", "", "",
                        "");
                return bidResultPayload;
            }
        };
        try {
            return auctionThreads[bidData.getAuctionId() % AUCTION_SHARDS].submit(placeBidTask).get();
        } catch (Exception e) {
            logger.error("Lỗi đặt giá", e);
            ResultPayload bidResultPayload = new ResultPayload(false, "Lỗi hệ thống máy chủ!", -1, "", "", "", "", "");
            return bidResultPayload;
        }
    }

    public void handleAutoBid(int auctionId) {
        Runnable autoBidTask = () -> {
            try {
                Auction currentAuction = dao.getAuctionById(auctionId);
                if (currentAuction == null)
                    throw new IllegalStateException("Không tìm thấy phiên đấu giá này!");
                List<AutoBidPayload> autoBids = dao.getActiveAutoBids(auctionId);
                if (autoBids.isEmpty())
                    return;

                double currentHighest = currentAuction.getItem().getCurHighest();
                int highestBidderId = currentAuction.getHighestBidderId();

                if (autoBids.size() == 1) {
                    if (autoBids.get(0).getUserId() == highestBidderId)
                        return;
                    else if (autoBids.get(0).getMaxAmount() < currentHighest) {
                        dao.cancelAutoBid(auctionId, autoBids.get(0).getUserId());
                    } else {
                        double maxAmount = autoBids.get(0).getMaxAmount();
                        double intendedAmount = currentHighest + autoBids.get(0).getIncrement();
                        if (intendedAmount > maxAmount)
                            intendedAmount = maxAmount;
                        Bidder bidder = new Bidder(autoBids.get(0).getUserId(), null, null);
                        BidTransaction newBid = new BidTransaction(0, bidder, intendedAmount, LocalDateTime.now(),
                                "Đặt giá qua 🤖 Auto-Bid");
                        boolean isBidSuccess = auctionService.placeBid(currentAuction, newBid);

                        if (isBidSuccess) {
                            announcer.notify(auctionId, intendedAmount);
                        }
                    }
                } else {
                    int winner = autoBids.get(0).getMaxAmount() > autoBids.get(1).getMaxAmount() ? 0 : 1;
                    int loser = winner ^ 1; // XOR để ra id autobid còn lại
                    double maxAmount = autoBids.get(winner).getMaxAmount();
                    double intendedAmount = autoBids.get(loser).getMaxAmount() + autoBids.get(winner).getIncrement();
                    if (intendedAmount > maxAmount)
                        intendedAmount = maxAmount;
                    Bidder bidder = new Bidder(autoBids.get(winner).getUserId(), null, null);
                    BidTransaction newBid = new BidTransaction(0, bidder, intendedAmount, LocalDateTime.now(),
                            "Đặt giá qua 🤖 Auto-Bid");
                    boolean isBidSuccess = auctionService.placeBid(currentAuction, newBid);
                    if (isBidSuccess) {
                        announcer.notify(auctionId, intendedAmount);
                    }
                }
            } catch (IllegalStateException e) {
                logger.error("Lỗi đặt autobid", e);
            }
        };

        try {
            auctionThreads[auctionId % AUCTION_SHARDS].submit(autoBidTask);
        } catch (Exception e) {
            logger.error("Lỗi đặt giá", e);
        }
    }
}
