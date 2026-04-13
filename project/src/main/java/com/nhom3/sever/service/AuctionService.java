package com.nhom3.sever.service;

import com.nhom3.shared.model.Auction.Auction;
import com.nhom3.shared.model.Auction.BidTransaction;
import com.nhom3.shared.model.Auction.StatusOfAuction;

public class AuctionService {
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
                    + (auction.getHighestBidder() != null ? auction.getHighestBidder().getName()
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
        if (auction.getStatus() == StatusOfAuction.RUNNING) {
            if (auction.getHighestBidder() == null || bid.getBidAmount() > auction.getItem().getCurHighest()) {
                auction.setHighestBidder(bid.getBidder());
                auction.getItem().setCurHighest(bid.getBidAmount());
                auction.getBidHistory().add(bid);
                return true;
            } else {
                return false;
            }
        } else return false;
    }
}
