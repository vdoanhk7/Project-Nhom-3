package com.nhom3.sever.service;

import java.time.LocalDateTime;

import com.nhom3.sever.dao.AuctionDAO;
import com.nhom3.sever.dao.AuctionDAOImpl;
import com.nhom3.shared.model.auction.Auction;
import com.nhom3.shared.model.auction.BidTransaction;
import com.nhom3.shared.model.auction.StatusOfAuction;
import com.nhom3.shared.model.item.Item;
import com.nhom3.shared.model.user.Seller;

public class AuctionService {
    private final AuctionDAO auctionDAO;
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
            
            auction.getItem().setCurHighest(bid.getAmount());
            auction.setHighestBidder(bid.getBidder());
            auction.getBidHistory().add(bid);
            System.out.println("Server: Đặt giá thành công: " + bid.getAmount() + " bởi " + bid.getBidder().getUserInfo().getName());
            return true;
        } else {
            System.out.println("Server: Đặt giá thất bại: " + bid.getAmount() + " bởi " + bid.getBidder().getUserInfo().getName());
            return false;
        }
        // if (auction.getStatus() == StatusOfAuction.RUNNING) {
        //     if (auction.getHighestBidder() == null || bid.getAmount() > auction.getItem().getCurHighest()) {
        //         auction.setHighestBidder(bid.getBidder());
        //         auction.getItem().setCurHighest(bid.getAmount());
        //         auction.getBidHistory().add(bid);
        //         return true;
        //     } else {
        //         return false;
        //     }
        // } else return false;
    }
}
