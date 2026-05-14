package com.nhom3.server.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.nhom3.server.dao.AuctionDAO;
import com.nhom3.server.dao.AuctionDAOImpl;
import com.nhom3.shared.model.auction.Auction;
import com.nhom3.shared.model.auction.BidTransaction;
import com.nhom3.shared.model.auction.StatusOfAuction;
import com.nhom3.shared.network.payload.AutoBidPayload;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AuctionService {
    private final AuctionDAO auctionDAO;
    private static final int SNIPE_THRESHOLD_SECONDS = 30;
    private static final int EXTENSION_MINUTES = 2;
    private static final Logger log = LoggerFactory.getLogger(AuctionService.class);

        // Bộ lập lịch để tạo khoảng trễ (Delay) giữa các lần Bot tự động đặt giá
    private static final ScheduledExecutorService autoBidScheduler = Executors.newScheduledThreadPool(10);

    // Khóa đồng bộ theo từng Auction ID
    private static final Map<Integer, Object> auctionLocks = new ConcurrentHashMap<>();

    // Cờ đánh dấu xem phiên này đang có Bot nào "chuẩn bị" đặt giá hay không (Chống spam request)
    private static final Map<Integer, Boolean> isAutoBidProcessing = new ConcurrentHashMap<>();

    public AuctionService() {
        this.auctionDAO = new AuctionDAOImpl();
    }

    // Constructor for testing
    public AuctionService(AuctionDAO auctionDAO) {
        this.auctionDAO = auctionDAO;
    }
    
    public boolean createAuction(Auction auction) throws IllegalArgumentException, IllegalStateException {

        if (auction == null) {
            throw new IllegalArgumentException("Dữ liệu không hợp lệ!");
        }
        if (auction.getItem() == null) {
            throw new IllegalArgumentException("Sản phẩm không hợp lệ!");
        }
        if (auction.getItem().getId() <= 0) {
            throw new IllegalArgumentException("ID sản phẩm không hợp lệ!");
        }
        log.info("Bắt đầu yêu cầu tạo phiên đấu giá cho sản phẩm ID: {}", auction.getItem().getId());


        if (auction.getEndTime().isBefore(auction.getStartTime())) {
            log.warn("Từ chối tạo: Thời gian kết thúc ({}) trước thời gian bắt đầu ({}).", auction.getEndTime(), auction.getStartTime());
            throw new IllegalArgumentException("Thời gian kết thúc phải sau thời gian bắt đầu!");
        }

        // Một sản phẩm không thể có 2 phiên đấu giá chạy cùng lúc
        Auction existingAuction = auctionDAO.getAuctionByItemId(auction.getItem().getId());
        if (existingAuction != null) {
            StatusOfAuction currentStatus = existingAuction.getStatus();
            if (currentStatus == StatusOfAuction.OPEN || currentStatus == StatusOfAuction.RUNNING) {
                log.warn("Từ chối tạo: Sản phẩm ID {} đang có phiên đấu giá chưa kết thúc.", auction.getItem().getId());
                throw new IllegalStateException("Sản phẩm này đang có một phiên đấu giá chưa kết thúc!");
            }
        }

        // Nếu người dùng hẹn giờ trong tương lai -> OPEN
        // Nếu người dùng chọn giờ bắt đầu là ngay bây giờ (hoặc quá khứ do độ trễ mạng) -> RUNNING
        LocalDateTime now = LocalDateTime.now();
        if (auction.getStartTime().isAfter(now)) {
            auction.setStatus(StatusOfAuction.OPEN);
        } else {
            auction.setStatus(StatusOfAuction.RUNNING);
        }

        boolean isSuccess = auctionDAO.createAuction(auction);

        if (isSuccess) {
            log.info("Tạo thành công phiên đấu giá ID: {} cho sản phẩm ID: {}", auction.getId(), auction.getItem().getId());
            return true;
        } else {
            log.error("Lỗi Database khi tạo phiên đấu giá cho sản phẩm ID: {}", auction.getItem().getId());
            return false;
        }
    }

    public boolean startAuction(Auction auction) {
        
        if (auction.getStatus() == StatusOfAuction.RUNNING) {
            log.info("Phiên đấu giá đang chạy.");
            return false;
        } else if (auction.getStatus() == StatusOfAuction.FINISHED || auction.getStatus() == StatusOfAuction.PAID) {
            log.info("Phiên đấu giá đã kết thúc.");
            return false;
        } else if (auction.getStatus() == StatusOfAuction.CANCELLED) {
            log.info("Phiên đấu giá đã bị hủy.");
            return false;
        }
        
        boolean isSuccess = auctionDAO.startAuction(auction.getId());

        if (isSuccess) {
            log.info("Phiên đấu giá bắt đầu.");
            return true;
        } else {
            log.info("Bắt đầu phiên đấu giá thất bại.");
            return false;
        }

    }

    public boolean endAuction(Auction auction) {

        if (auction.getStatus() == StatusOfAuction.OPEN) {
            log.info("Phiên đấu giá chưa bắt đầu.");
            return false;
        } else if (auction.getStatus() == StatusOfAuction.FINISHED || auction.getStatus() == StatusOfAuction.PAID) {
            log.info("Phiên đấu giá đã kết thúc.");
            return false;
        } else if (auction.getStatus() == StatusOfAuction.CANCELLED) {
            log.info("Phiên đấu giá đã bị hủy.");
            return false;
        }
        
        boolean isSuccess = auctionDAO.endAuction(auction.getId());

        if (isSuccess) {
            log.info("Phiên đấu giá đã kết thúc thành công.");
            return true;
        } else {
            log.info("Kết thúc phiên đấu giá thất bại.");
            return false;
        }
    }

    public boolean cancelAuction(int auctionId) throws IllegalStateException {
        
        boolean isSuccess = auctionDAO.cancelAuction(auctionId);

        if (isSuccess) {
            log.info("[Server] Đã hủy thành công phiên đấu giá ID: " + auctionId);
            return true;
        } else {
            log.info("[Server] Hủy thất bại phiên đấu giá ID: " + auctionId);
            throw new IllegalStateException("Không thể hủy! Phiên đấu giá đã kết thúc hoặc đã bị hủy trước đó.");
        }
    }

    private Object getLock(int auctionId) {
        return auctionLocks.computeIfAbsent(auctionId, k -> new Object());
    }

    // Hàm gọi từ ClientHandler khi có người bấm Đặt giá thủ công
    public boolean placeBid(Auction auction, BidTransaction bid) {
        synchronized (getLock(auction.getId())) {
            // Thực hiện đặt giá
            boolean isSuccess = placeBidInternal(auction, bid);
            
            // Dù người thật vừa đặt, hãy gọi Bot kiểm tra xem có cần đáp trả không
            if (isSuccess) {
                triggerAutoBids(auction.getId());
            }
            return isSuccess;
        }
    }

    // Logic cốt lõi ghi vào DB
    private boolean placeBidInternal(Auction auction, BidTransaction bid) throws IllegalStateException {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(auction.getStartTime()) || now.isAfter(auction.getEndTime()) || auction.getStatus() == StatusOfAuction.CANCELLED) {
            return false; 
        }
        
        if (bid.getBidder().getId() == auction.getHighestBidderId()) {
            throw new IllegalStateException("Bạn đang là người dẫn đầu, không cần đặt thêm nhé!");
        }

        boolean isSuccess = auctionDAO.updateHighestBid(auction.getId(), bid.getBidder().getId(), bid.getAmount());
        
        if (isSuccess) {
            auctionDAO.saveBidTransaction(bid, auction.getId());
            
            auction.getItem().setCurHighest(bid.getAmount());
            auction.setHighestBidder(bid.getBidder());

            // Chống Snipe
            LocalDateTime realEndTime = auctionDAO.getEndTime(auction.getId());
            if (realEndTime != null) {
                long secondsLeft = ChronoUnit.SECONDS.between(now, realEndTime);
                if (secondsLeft > 0 && secondsLeft <= SNIPE_THRESHOLD_SECONDS) {
                    auctionDAO.extendAuctionTime(auction.getId(), EXTENSION_MINUTES);
                }
            }
            return true;
        }
        return false;
    }

    // BỘ MÁY XỬ LÝ AUTO-BID TỰ ĐỘNG (NHẢY GIÁ TỪ TỪ)
    public void triggerAutoBids(int auctionId) {
        // Nếu đã có 1 luồng Bot đang đếm ngược chờ đặt giá cho phiên này, thì bỏ qua không tạo thêm luồng để tránh loạn giá
        if (isAutoBidProcessing.getOrDefault(auctionId, false)) {
            return;
        }

        // Đánh dấu là Bot đang suy nghĩ
        isAutoBidProcessing.put(auctionId, true);

        // Hẹn giờ 1.5 giây sau Bot mới tung đòn đáp trả (Tạo hiệu ứng giá nhảy liên tục chân thực)
        autoBidScheduler.schedule(() -> {
            synchronized (getLock(auctionId)) {
                try {
                    Auction auction = auctionDAO.getAuctionById(auctionId);
                    if (auction == null) return;
                    
                    List<AutoBidPayload> autoBids = auctionDAO.getActiveAutoBids(auctionId);
                    if (autoBids.isEmpty()) return;

                    double currentHighest = auction.getItem().getCurHighest();
                    int highestBidderId = auction.getHighestBidderId();

                    boolean bidPlaced = false;

                    // Chỉ cho 1 Bot hợp lệ nhảy vào đáp trả ở nhịp này
                    for (AutoBidPayload config : autoBids) {
                        // Bỏ qua Bot của người đang dẫn đầu
                        if (config.getUserId() == highestBidderId) continue;

                        double nextBidAmount;
                        if (highestBidderId <= 0) {
                            // Yêu cầu 1: Chưa có ai đấu giá -> Đặt bằng giá khởi điểm
                            nextBidAmount = auction.getItem().getStartPrice();
                        } else {
                            // Yêu cầu 2: Đã có người đặt -> Đặt bằng giá cao nhất + Bước giá
                            nextBidAmount = currentHighest + config.getIncrement();
                        }

                        // Yêu cầu 3: Người có maxAmount cao hơn sẽ trụ lại (Chỉ đặt nếu giá tính toán <= Ví tiền cài đặt)
                        if (nextBidAmount <= config.getMaxAmount()) {
                            com.nhom3.shared.model.user.Bidder botBidder = new com.nhom3.shared.model.user.Bidder(config.getUserId(), null, null);
                            BidTransaction autoBidTx = new BidTransaction(0, botBidder, nextBidAmount, LocalDateTime.now(), "🤖 Auto-Bid");

                            // Thực hiện đặt giá
                            try {
                                bidPlaced = placeBidInternal(auction, autoBidTx);
                                if (bidPlaced) {
                                    com.nhom3.server.network.liveUpdate.Announcer.getInstance()
                                            .notify(auctionId, nextBidAmount);
                                    break; // Chỉ 1 Bot được đặt trong nhịp này. Thoát vòng lặp For.
                                }
                            } catch (IllegalStateException e) {
                                // Nếu Bot lỡ tự đặt đè lên chính nó, bỏ qua không làm sập hệ thống
                                bidPlaced = false;
                            }
                        }
                    }

                    // Nếu ở nhịp này có Bot vừa đặt giá thành công, ta GỌI LẠI CHÍNH HÀM NÀY
                    // Để 1.5 giây sau, các Bot khác có cơ hội đáp trả lại Bot vừa rồi.
                    if (bidPlaced) {
                        isAutoBidProcessing.put(auctionId, false); // Mở khóa cờ
                        triggerAutoBids(auctionId); // Kích hoạt nhịp tiếp theo
                    }

                } finally {
                    // Đảm bảo cờ luôn được tắt dù có lỗi xảy ra
                    isAutoBidProcessing.put(auctionId, false);
                }
            }
        }, 1500, TimeUnit.MILLISECONDS); // Delay 1.5 giây (1500 mili-giây)
    }
}
