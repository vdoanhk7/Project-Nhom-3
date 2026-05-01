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


public class AuctionService {
    private final AuctionDAO auctionDAO;
    private static final int SNIPE_THRESHOLD_SECONDS = 30;
    private static final int EXTENSION_MINUTES = 2;
    private static final Logger log = LoggerFactory.getLogger(AuctionService.class);

    public AuctionService() {
        this.auctionDAO = new AuctionDAOImpl();
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

    public synchronized boolean placeBid(Auction auction, BidTransaction bid) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(auction.getStartTime())) {
            throw new IllegalStateException("Phiên đấu giá chưa bắt đầu! Không thể đặt giá.");
        }
        if (now.isAfter(auction.getEndTime())) {
            throw new IllegalStateException("Phiên đấu giá đã kết thúc! Yêu cầu đặt giá bị từ chối.");
        }
        if (auction.getStatus() == StatusOfAuction.CANCELLED) {
            throw new IllegalStateException("Phiên đấu giá bị hủy! Yêu cầu đặt giá bị từ chối.");
        }
        if (bid.getBidder().getId() == auction.getHighestBidderId()) {
            throw new IllegalStateException("Bạn đã là người đặt giá cao nhất! Không thể đặt giá tiếp.");
        }

        boolean isSuccess = auctionDAO.updateHighestBid(auction.getId(), bid.getBidder().getId(), bid.getAmount());
        
        if (isSuccess) {
            auctionDAO.saveBidTransaction(bid, auction.getId());
            // Tránh lỗi thêm thời gian nhiều lần khi có nhiều người đặt giá cùng lúc
            LocalDateTime realEndTime = auctionDAO.getEndTime(auction.getId());
            if (realEndTime == null) return false;
            long secondsLeft = ChronoUnit.SECONDS.between(now, realEndTime);
            if (secondsLeft > 0 && secondsLeft <= SNIPE_THRESHOLD_SECONDS) {
                auctionDAO.extendAuctionTime(auction.getId(), EXTENSION_MINUTES);
            }
            return true;
        }
        return false;
    }

}
