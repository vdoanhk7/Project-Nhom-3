package com.nhom3;

import java.time.LocalDateTime;

class BidTransaction extends Entity{
    private String auctionId; // phien dau gia
    private String bidderId; // ma nguoi dat
    private double bidAmount; // tien dat
    private LocalDateTime bidTime; // thoi gian dat
    private String note;
    private String status;// trang thai dat

    public BidTransaction(String id, String auctionId, String bidderId, double bidAmount,
                          LocalDateTime bidTime, String note, String status) {
        super(id);
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidAmount = bidAmount;
        this.bidTime = bidTime;
        this.note = note;
        this.status = status;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }

    public void setBidderId(String bidderId) {
        this.bidderId = bidderId;
    }

    public void setBidAmount(double bidAmount) {
        this.bidAmount = bidAmount;
    }

    public void setBidTime(LocalDateTime bidTime) {
        this.bidTime = bidTime;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public double getBidAmount() {
        return bidAmount;
    }

    public LocalDateTime getBidTime() {
        return bidTime;
    }

    public String getBidderId() {
        return bidderId;
    }

    public String getNote() {
        return note;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public void displayInfo() {
        System.out.println("Mã giao dịch: " + id);
        System.out.println("Phiên đấu giá: " + auctionId);
        System.out.println("Người đặt: " + bidderId);
        System.out.println("Giá đặt: " + bidAmount);
        System.out.println("Thời gian đặt: " + bidTime);
        System.out.println("Ghi chú: " + note);
        System.out.println("Trạng thái: " + status);
    }

}
