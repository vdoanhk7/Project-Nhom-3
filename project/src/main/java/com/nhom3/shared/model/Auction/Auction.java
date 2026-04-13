package com.nhom3.shared.model.Auction;
import java.time.LocalDateTime;
import com.nhom3.shared.model.Item.Item;
import com.nhom3.shared.model.User.Bidder;
import java.util.ArrayList;
import java.util.List;

public class Auction{
    private String id;
    private Item item;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Bidder highestBidder;
    private StatusOfAuction status;
    private List<BidTransaction> bidHistory;
    
    public Auction(String id, Item item, LocalDateTime startTime, LocalDateTime endTime) {
        this.id = id;
        this.item = item;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = StatusOfAuction.OPEN;
        this.bidHistory = new ArrayList<>();
        
    }

    public String getId() {
        return id;
    }
    public void addBid(BidTransaction bid) {
        if (status == StatusOfAuction.RUNNING) {
            if (highestBidder == null || bid.getBidAmount() > item.getCurHighest()) {
                highestBidder = bid.getBidder();
                item.setCurHighest(bid.getBidAmount());
                bidHistory.add(bid);
                System.out.println("Them thanh cong");
            } else {
                System.out.println("Gia dat khong duoc thap hon gia hien tai");
            }
        }else {
            System.out.println("Phiên đấu giá đã kết thúc hoặc chưa bắt đầu.");
        }
    }

    public void endAuction() {
        if (status == StatusOfAuction.RUNNING) {
            status = StatusOfAuction.FINISHED;
            System.out.println("Phiên đấu giá đã kết thúc. Người thắng cuộc: " + (highestBidder != null ? highestBidder.getName() : "Không có người thắng"));
        } else if (status == StatusOfAuction.OPEN) {
            System.out.println("Phiên đấu giá chưa bắt đầu.");
        } else if (status == StatusOfAuction.FINISHED || status == StatusOfAuction.PAID) {
            System.out.println("Phiên đấu giá đã kết thúc.");
        } else if (status == StatusOfAuction.CANCELLED) {
            System.out.println("Phiên đấu giá đã bị hủy.");
        }
    }
    public void runAuction() {
        if (status == StatusOfAuction.OPEN) {
            status = StatusOfAuction.RUNNING;
            System.out.println("Phiên đấu giá đã bắt đầu.");
        } else if (status == StatusOfAuction.RUNNING) {
            System.out.println("Phiên đấu giá đang chạy.");
        } else if (status == StatusOfAuction.FINISHED || status == StatusOfAuction.PAID) {
            System.out.println("Phiên đấu giá đã kết thúc.");
        } else if (status == StatusOfAuction.CANCELLED) {
            System.out.println("Phiên đấu giá đã bị hủy.");
        }
    }
    public void cancelAuction() {
        if (status == StatusOfAuction.OPEN || status == StatusOfAuction.RUNNING) {
            status = StatusOfAuction.CANCELLED;
            System.out.println("Phiên đấu giá đã bị hủy.");
        } else if (status == StatusOfAuction.FINISHED || status == StatusOfAuction.PAID) {
            System.out.println("Phiên đấu giá đã kết thúc, không thể hủy.");
        } else if (status == StatusOfAuction.CANCELLED) {
            System.out.println("Phiên đấu giá đã bị hủy.");
        }
    }

    public Item getItem() {
        return item;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public Bidder getHighestBidder() {
        return highestBidder;
    }

    public StatusOfAuction getStatus() {
        return status;
    }

    public List<BidTransaction> getBidHistory() {
        return bidHistory;
    }

    
}
