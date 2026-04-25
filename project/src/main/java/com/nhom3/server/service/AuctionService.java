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

    public void cancelAuction(Auction auction) {
        if (auction.getStatus() == StatusOfAuction.OPEN || auction.getStatus() == StatusOfAuction.RUNNING) {
            auction.setStatus(StatusOfAuction.CANCELLED);
            System.out.println("Phiên đấu giá đã bị hủy.");
        } else if (auction.getStatus() == StatusOfAuction.FINISHED || auction.getStatus() == StatusOfAuction.PAID) {
            System.out.println("Phiên đấu giá đã kết thúc, không thể hủy.");
        } else if (auction.getStatus() == StatusOfAuction.CANCELLED) {
            System.out.println("Phiên đấu giá đã bị hủy.");
        }
    }

    public synchronized boolean placeBid(Auction auction, BidTransaction bid) {
         boolean isSuccess = auctionDAO.updateHighestBid(auction.getId(), bid.getBidder().getId(), bid.getAmount());
        
        if (isSuccess) {
            auctionDAO.saveBidTransaction(bid, auction.getId());
            
            LocalDateTime now = LocalDateTime.now();
            // Tính khoảng cách giữa hiện tại và lúc kết thúc (theo giây)
            long secondsLeft = ChronoUnit.SECONDS.between(now, auction.getEndTime());
            
            if (secondsLeft > 0 && secondsLeft <= SNIPE_THRESHOLD_SECONDS) {
                System.out.println("[Anti-Snipe] Phát hiện đặt giá sát giờ! Gia hạn thêm " + EXTENSION_MINUTES + " phút.");
                auctionDAO.extendAuctionTime(auction.getId(), EXTENSION_MINUTES);                
                auction.setEndTime(auction.getEndTime().plusMinutes(EXTENSION_MINUTES));
            }
            return true;
        }
        return false;
    }

}
