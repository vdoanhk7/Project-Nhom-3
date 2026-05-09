package com.nhom3.server.network;

import com.nhom3.server.dao.AuctionDAOImpl;
import com.nhom3.server.service.AuctionService;
import com.nhom3.shared.model.auction.Auction;
import com.nhom3.shared.model.auction.BidTransaction;
import com.nhom3.shared.model.user.Bidder;
import com.nhom3.shared.network.payload.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Callable;
import java.util.Set;

public class AuctionHandler {
    private static final Logger logger = LoggerFactory.getLogger(AuctionHandler.class);
    private static final int AUCTION_SHARDS = 10;
    private static AuctionHandler instance;

    private final AuctionDAOImpl dao;
    private final AuctionService auctionService;
    private final ConcurrentHashMap<Integer, Set<Integer>> clientInAuctions;
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
        clientInAuctions = new ConcurrentHashMap<>();
        auctionThreads = new ExecutorService[AUCTION_SHARDS];
        for (int i = 0; i < AUCTION_SHARDS; i++) {
            auctionThreads[i] = Executors.newSingleThreadExecutor();
        }
    }

    // Manages client connections to auctions
    public boolean addClientToAuction(int clientId, int auctionId) {
        boolean success = clientInAuctions.get(auctionId).add(clientId);
        if (success) {
            logger.info("Client {} added to auction {}", clientId, auctionId);
        } else {
            logger.info("Client {} already in auction {}", clientId, auctionId);
        }
        return success;
    }

    public boolean removeClientFromAuction(int clientId, int auctionId) {
        boolean success = clientInAuctions.get(auctionId).remove(clientId);
        if (success) {
            logger.info("Client {} removed from auction {}", clientId, auctionId);
        } else {
            logger.info("Client {} not in auction {}", clientId, auctionId);
        }
        return success;
    }

    // Handle Bid/AutoBid
    public ResultPayload handleBid(BidPayload bidData) {
        Callable<ResultPayload> placeBidTask = () -> {
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
                return bidResultPayload;
            } catch (IllegalStateException e) {
                ResultPayload bidResultPayload = new ResultPayload(false, e.getMessage(), -1, "", "", "", "", "");
                return bidResultPayload;
            } catch (Exception e) {
                e.printStackTrace();
                ResultPayload bidResultPayload = new ResultPayload(false, "Lỗi hệ thống máy chủ!", -1, "", "", "", "", "");
                return bidResultPayload;
            }
        };
        try {
            return auctionThreads[bidData.getAuctionId() % AUCTION_SHARDS].submit(placeBidTask).get();
        } catch (Exception e) {
            e.printStackTrace();
            ResultPayload bidResultPayload = new ResultPayload(false, "Lỗi hệ thống máy chủ!", -1, "", "", "", "", "");
            return bidResultPayload;
        }
    }


}
