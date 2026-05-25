package com.nhom3.server.network;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nhom3.server.dao.AuctionDAOImpl;
import com.nhom3.server.exception.AuctionNotFoundException;
import com.nhom3.server.exception.BusinessRuleException;
import com.nhom3.server.network.liveUpdate.Announcer;
import com.nhom3.server.service.AuctionService;
import com.nhom3.shared.model.auction.Auction;
import com.nhom3.shared.model.auction.BidTransaction;
import com.nhom3.shared.model.user.Bidder;
import com.nhom3.shared.network.payload.AutoBidPayload;
import com.nhom3.shared.network.payload.BidHistoryResponsePayload;
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

    // Handle Bid/AutoBid
    public ResultPayload handleBid(BidPayload bidData) {
        Callable<ResultPayload> placeBidTask;
        placeBidTask = () -> {
            try {
                Auction currentAuction = dao.getAuctionById(bidData.getAuctionId());
                if (currentAuction == null)
                    throw new AuctionNotFoundException(
                            "AUCTION_NOT_FOUND",
                            "Không tìm thấy phiên đấu giá này!");

                Bidder bidder = new Bidder(bidData.getUserId(), null, null);
                BidTransaction newBid = new BidTransaction(0, bidder, bidData.getAmount(), LocalDateTime.now(),
                        "Đặt giá qua mạng");
                LocalDateTime previousEndTime = currentAuction.getEndTime();
                boolean isBidSuccess = auctionService.placeBid(currentAuction, newBid);

                ResultPayload bidResultPayload = new ResultPayload(isBidSuccess,
                        isBidSuccess ? "Đặt giá thành công" : "Có người đã trả giá cao hơn, vui lòng thử lại!", -1, "",
                        "", "", "", "");
                if (isBidSuccess) {
                    publishPriceUpdateSnapshot(currentAuction, previousEndTime, newBid);
                }
                return bidResultPayload;
            } catch (BusinessRuleException e) {
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
                    throw new AuctionNotFoundException(
                            "AUCTION_NOT_FOUND",
                            "Không tìm thấy phiên đấu giá này!");
                if (currentAuction.getStatus() != com.nhom3.shared.model.auction.StatusOfAuction.RUNNING)
                    return;

                List<AutoBidPayload> autoBids = dao.getActiveAutoBids(auctionId);
                if (autoBids.isEmpty())
                    return;

                double currentHighest = currentAuction.getItem().getCurHighest();
                int highestBidderId = currentAuction.getHighestBidderId();
                boolean cancelledAutoBid = false;

                // Dùng Map để lưu thứ tự đăng ký (đảm bảo người đăng ký trước thắng nếu bằng giá)
                java.util.Map<Integer, Integer> timeOrder = new java.util.HashMap<>();
                for (int i = 0; i < autoBids.size(); i++) {
                    timeOrder.put(autoBids.get(i).getUserId(), i);
                }

                // Hàng đợi ưu tiên: So sánh maxAmount giảm dần. Nếu bằng nhau, ưu tiên người đăng ký trước.
                java.util.PriorityQueue<AutoBidPayload> pq = new java.util.PriorityQueue<>((a, b) -> {
                    int maxCmp = Double.compare(b.getMaxAmount(), a.getMaxAmount());
                    if (maxCmp != 0) return maxCmp;
                    return Integer.compare(timeOrder.get(a.getUserId()), timeOrder.get(b.getUserId()));
                });
                pq.addAll(autoBids);

                AutoBidPayload winnerBot = pq.poll();
                if (winnerBot == null) return;

                double bidStep = currentAuction.getBidStep();
                AutoBidPayload secondBot = null;
                
                // Tìm đối thủ mạnh nhất còn khả năng trả giá
                while (!pq.isEmpty()) {
                    AutoBidPayload bot = pq.poll();
                    if (bot.getMaxAmount() >= currentHighest + bidStep) {
                        secondBot = bot;
                        break;
                    } else {
                        cancelledAutoBid |= dao.cancelAutoBid(auctionId, bot.getUserId()); // Bot này hết tiền, hủy.
                    }
                }

                if (secondBot == null) {
                    // Chưa có ai đặt giá nào, bot này sẽ là người dẫn đầu với giá khởi điểm
                    if (highestBidderId == -1) {
                        double startingPrice = currentAuction.getItem().getStartPrice();

                        Bidder bidder = new Bidder(winnerBot.getUserId(), null, null);
                        BidTransaction newBid = new BidTransaction(
                                0, bidder, startingPrice, LocalDateTime.now(), "Đặt giá qua \ud83e\udd16 Auto-Bid");
                        LocalDateTime previousEndTime = currentAuction.getEndTime();
                        boolean isBidSuccess = auctionService.placeAutoBid(currentAuction, newBid);
                        if (isBidSuccess) {
                            publishPriceUpdateSnapshot(currentAuction, previousEndTime, newBid);
                        }
                        return;
                    }
                    // CHỈ CÒN 1 NGƯỜI DUY NHẤT
                    if (winnerBot.getUserId() == highestBidderId) {
                        if (cancelledAutoBid) {
                            publishAuctionSnapshot(currentAuction, "AUTO_BID_CONFIG_CHANGED", null);
                        }
                        return; // Đang dẫn đầu, không cần tự tự nâng giá mình lên
                    }
                    
                    double requiredMin = currentHighest + bidStep;
                    if (winnerBot.getMaxAmount() < requiredMin) {
                        cancelledAutoBid |= dao.cancelAutoBid(auctionId, winnerBot.getUserId());
                        if (cancelledAutoBid) {
                            publishAuctionSnapshot(currentAuction, "AUTO_BID_CONFIG_CHANGED", null);
                        }
                        return;
                    }
                    
                    double intendedAmount = currentHighest + winnerBot.getIncrement();
                    if (intendedAmount < requiredMin) {
                        intendedAmount = requiredMin;
                    }
                    if (intendedAmount > winnerBot.getMaxAmount()) {
                        intendedAmount = winnerBot.getMaxAmount();
                    }

                    Bidder bidder = new Bidder(winnerBot.getUserId(), null, null);
                    BidTransaction newBid = new BidTransaction(
                            0, bidder, intendedAmount, LocalDateTime.now(), "Đặt giá qua \ud83e\udd16 Auto-Bid");
                    LocalDateTime previousEndTime = currentAuction.getEndTime();
                    boolean isBidSuccess = auctionService.placeAutoBid(currentAuction, newBid);
                    if (isBidSuccess) {
                        publishPriceUpdateSnapshot(currentAuction, previousEndTime, newBid);
                    } else if (cancelledAutoBid) {
                        publishAuctionSnapshot(currentAuction, "AUTO_BID_CONFIG_CHANGED", null);
                    }
                } else {
                    // CÓ 2 NGƯỜI ĐẤU VỚI NHAU TRỞ LÊN
                    // Giá cuối cùng sẽ được đẩy lên bằng giá max của người thứ 2 + bước nhảy của người thứ 1
                    double intendedAmount = secondBot.getMaxAmount() + winnerBot.getIncrement();
                    
                    double requiredMin = currentHighest + bidStep;
                    if (intendedAmount < requiredMin) {
                        intendedAmount = requiredMin;
                    }
                    if (intendedAmount > winnerBot.getMaxAmount()) {
                        intendedAmount = winnerBot.getMaxAmount();
                    }

                    // Hủy người thua cuộc (secondBot) và tất cả các bot yếu hơn trong hàng đợi
                    cancelledAutoBid |= dao.cancelAutoBid(auctionId, secondBot.getUserId());
                    while (!pq.isEmpty()) {
                        cancelledAutoBid |= dao.cancelAutoBid(auctionId, pq.poll().getUserId());
                    }

                    // Chốt giá chiến thắng cho winnerBot (chỉ gọi 1 lần duy nhất vào DB, tránh spam đệ quy)
                    Bidder bidder = new Bidder(winnerBot.getUserId(), null, null);
                    BidTransaction newBid = new BidTransaction(
                            0, bidder, intendedAmount, LocalDateTime.now(), "Đặt giá qua \ud83e\udd16 Auto-Bid");
                    LocalDateTime previousEndTime = currentAuction.getEndTime();
                    boolean isBidSuccess = auctionService.placeAutoBid(currentAuction, newBid);
                    if (isBidSuccess) {
                        publishPriceUpdateSnapshot(currentAuction, previousEndTime, newBid);
                    } else if (cancelledAutoBid) {
                        publishAuctionSnapshot(currentAuction, "AUTO_BID_CONFIG_CHANGED", null);
                    }
                }
            } catch (BusinessRuleException e) {
                logger.error("Lỗi đặt autobid", e);
            }
        };

        try {
            auctionThreads[auctionId % AUCTION_SHARDS].submit(autoBidTask);
        } catch (Exception e) {
            logger.error("Lỗi đặt giá", e);
        }
    }

    private void publishPriceUpdateSnapshot(Auction auction, LocalDateTime previousEndTime, BidTransaction latestBid) {
        syncAuctionEndTimeFromDatabase(auction);
        String eventType = auction != null
                && previousEndTime != null
                && auction.getEndTime() != null
                && auction.getEndTime().isAfter(previousEndTime)
                        ? "AUCTION_EXTENDED"
                        : "PRICE_UPDATED";
        publishAuctionSnapshot(auction, eventType, latestBid);
    }

    private void syncAuctionEndTimeFromDatabase(Auction auction) {
        if (auction == null) {
            return;
        }

        LocalDateTime latestEndTime = dao.getEndTime(auction.getId());
        if (latestEndTime != null) {
            auction.setEndTime(latestEndTime);
        }
    }

    private void publishAuctionSnapshot(Auction auction, String eventType, BidTransaction latestBid) {
        BidHistoryResponsePayload.SimpleBid simpleBid = null;
        if (latestBid != null) {
            int bidderId = latestBid.getBidder() != null ? latestBid.getBidder().getId() : -1;
            String bidderName = bidderId > 0 ? dao.getBidderDisplayName(bidderId) : null;
            if (bidderName == null || bidderName.isBlank()) {
                bidderName = bidderId > 0 ? "Bidder #" + bidderId : "Ẩn danh";
            }
            simpleBid = new BidHistoryResponsePayload.SimpleBid(
                    latestBid.getAmount(),
                    latestBid.getBidTime().toString(),
                    latestBid.getNote(),
                    bidderName);
        }
        announcer.notifyAuctionSnapshot(auction, eventType, null, simpleBid);
    }
}
