package com.nhom3.server.network;

import java.time.LocalDateTime;
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

    public static AuctionHandler getInstance() {
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
                ResultPayload bidResultPayload = new ResultPayload(false, "Lỗi hệ thống máy chủ!", -1, "", "", "", "", "");
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
}
