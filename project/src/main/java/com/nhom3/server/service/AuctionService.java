package com.nhom3.server.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import com.nhom3.server.dao.AuctionDAO;
import com.nhom3.server.dao.AuctionDAOImpl;
import com.nhom3.shared.model.auction.Auction;
import com.nhom3.shared.model.auction.BidTransaction;
import com.nhom3.shared.model.auction.StatusOfAuction;
import com.nhom3.shared.model.item.Item;
import com.nhom3.shared.model.user.Seller;

public class AuctionService {
    private final AuctionDAO auctionDAO;
    private static final int SNIPE_THRESHOLD_SECONDS = 30;
    private static final int EXTENSION_MINUTES = 2;

    public AuctionService() {
        this.auctionDAO = new AuctionDAOImpl();
    }
    public void createAuction(Seller seller, Item item, int id, LocalDateTime startTime, LocalDateTime endTime) {
        if (seller == null || item == null) {
            System.out.println("Seller hoặc Item không tồn tại.");
            return;
        }
        Auction newAuction = new Auction(id, item, startTime, endTime);
        seller.getManagedAuctions().add(newAuction);
    }
    public void startAuction(Auction auction) {
        if (auction.getStatus() == StatusOfAuction.OPEN) {
            auction.setStatus(StatusOfAuction.RUNNING);
            System.out.println("Phiên đấu giá đã bắt đầu.");
        } else if (auction.getStatus() == StatusOfAuction.RUNNING) {
            System.out.println("Phiên đấu giá đang chạy.");
        } else if (auction.getStatus() == StatusOfAuction.FINISHED || auction.getStatus() == StatusOfAuction.PAID) {
            System.out.println("Phiên đấu giá đã kết thúc.");
        } else if (auction.getStatus() == StatusOfAuction.CANCELLED) {
            System.out.println("Phiên đấu giá đã bị hủy.");
        }
    }

    public void endAuction(Auction auction) {
        if (auction.getStatus() == StatusOfAuction.RUNNING) {
            auction.setStatus(StatusOfAuction.FINISHED);
            System.out.println("Phiên đấu giá đã kết thúc. Người thắng cuộc: "
                    + (auction.getHighestBidder() != null ? auction.getHighestBidder().getUserInfo().getName()
                            : "Không có người thắng"));
        } else if (auction.getStatus() == StatusOfAuction.OPEN) {
            System.out.println("Phiên đấu giá chưa bắt đầu.");
        } else if (auction.getStatus() == StatusOfAuction.FINISHED || auction.getStatus() == StatusOfAuction.PAID) {
            System.out.println("Phiên đấu giá đã kết thúc.");
        } else if (auction.getStatus() == StatusOfAuction.CANCELLED) {
            System.out.println("Phiên đấu giá đã bị hủy.");
        }
    }

    public boolean cancelAuction(int auctionId) throws IllegalStateException {
        
        boolean isSuccess = auctionDAO.cancelAuction(auctionId);

        if (isSuccess) {
            System.out.println("[Server] Đã hủy thành công phiên đấu giá ID: " + auctionId);
            return true;
        } else {
            System.out.println("[Server] Hủy thất bại phiên đấu giá ID: " + auctionId);
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
