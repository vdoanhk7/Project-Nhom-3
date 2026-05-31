package com.nhom3.server.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nhom3.server.dao.AuctionDAO;
import com.nhom3.server.dao.AuctionDAOImpl;
import com.nhom3.server.exception.AuctionConflictException;
import com.nhom3.server.exception.BidRejectedException;
import com.nhom3.server.exception.InvalidAuctionException;
import com.nhom3.server.network.AuctionHandler;
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

    // Constructor for testing
    public AuctionService(AuctionDAO auctionDAO) {
        this.auctionDAO = auctionDAO;
    }
    
    public boolean createAuction(Auction auction) throws InvalidAuctionException, AuctionConflictException {
        return createAuction(auction, -1);
    }

    public boolean createAuction(Auction auction, int sellerId) throws InvalidAuctionException, AuctionConflictException {

        if (auction == null) {
            throw new InvalidAuctionException("AUCTION_INVALID_DATA", "Dữ liệu không hợp lệ!");
        }
        if (auction.getItem() == null) {
            throw new InvalidAuctionException("AUCTION_INVALID_ITEM", "Sản phẩm không hợp lệ!");
        }
        if (auction.getItem().getId() <= 0) {
            throw new InvalidAuctionException("AUCTION_INVALID_ITEM_ID", "ID sản phẩm không hợp lệ!");
        }
        log.info("Bắt đầu yêu cầu tạo phiên đấu giá cho sản phẩm ID: {}", auction.getItem().getId());


        if (auction.getEndTime().isBefore(auction.getStartTime())) {
            log.warn("Từ chối tạo: Thời gian kết thúc ({}) trước thời gian bắt đầu ({}).", auction.getEndTime(), auction.getStartTime());
            throw new InvalidAuctionException("AUCTION_INVALID_TIME", "Thời gian kết thúc phải sau thời gian bắt đầu!");
        }
        if (auction.getBidStep() <= 0) {
            throw new InvalidAuctionException("AUCTION_INVALID_BID_STEP", "Bước giá phải lớn hơn 0!");
        }

        // Một sản phẩm không thể có 2 phiên đấu giá chạy cùng lúc
        Auction existingAuction = auctionDAO.getAuctionByItemId(auction.getItem().getId());
        if (existingAuction != null) {
            StatusOfAuction currentStatus = getRealAuctionStatus(existingAuction);
            if (currentStatus == StatusOfAuction.OPEN || currentStatus == StatusOfAuction.RUNNING) {
                log.warn("Từ chối tạo: Sản phẩm ID {} đang có phiên đấu giá chưa kết thúc.", auction.getItem().getId());
                throw new AuctionConflictException(
                        "AUCTION_ALREADY_ACTIVE",
                        "Sản phẩm này đang có một phiên đấu giá chưa kết thúc!");
            }
            if (currentStatus == StatusOfAuction.FINISHED && existingAuction.getHighestBidderId() > 0) {
                boolean cancelledForRelist = sellerId > 0
                        && auctionDAO.cancelLatestUnpaidAuctionForRelist(auction.getItem().getId(), sellerId);
                if (!cancelledForRelist) {
                    log.warn("Từ chối đăng bán lại: Sản phẩm ID {} đã có người thắng ở phiên gần nhất.",
                            auction.getItem().getId());
                    throw new AuctionConflictException(
                            "AUCTION_FINISHED_HAS_WINNER",
                            "Phiên đấu giá đã có người thắng. Chỉ được đăng bán lại sau 2 ngày quá hạn thanh toán.");
                }
                log.info("Đã hủy giao dịch quá hạn để đăng bán lại sản phẩm ID {}.", auction.getItem().getId());
            }
            if (currentStatus == StatusOfAuction.PAID) {
                log.warn("Từ chối đăng bán lại: Sản phẩm ID {} đã được xác nhận thanh toán.",
                        auction.getItem().getId());
                throw new AuctionConflictException(
                        "AUCTION_ALREADY_PAID",
                        "Sản phẩm đã được xác nhận thanh toán, không thể đăng bán lại!");
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
            log.info("[OOP printInfo] Created auction detail: {}", auction.printInfo());
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
        } else if (auction.getStatus() == StatusOfAuction.FINISHED
                || auction.getStatus() == StatusOfAuction.PAID
                || auction.getStatus() == StatusOfAuction.DEAL_CANCELLED) {
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
        } else if (auction.getStatus() == StatusOfAuction.FINISHED
                || auction.getStatus() == StatusOfAuction.PAID
                || auction.getStatus() == StatusOfAuction.DEAL_CANCELLED) {
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

    public boolean cancelAuction(int auctionId) throws AuctionConflictException {
        
        boolean isSuccess = auctionDAO.cancelAuction(auctionId);

        if (isSuccess) {
            log.info("[Server] Đã hủy thành công phiên đấu giá ID: " + auctionId);
            return true;
        } else {
            log.info("[Server] Hủy thất bại phiên đấu giá ID: " + auctionId);
            throw new AuctionConflictException(
                    "AUCTION_CANCEL_NOT_ALLOWED",
                    "Không thể hủy! Phiên đấu giá đã kết thúc hoặc đã bị hủy trước đó.");
        }
    }

    // Hàm gọi từ ClientHandler khi có người bấm Đặt giá thủ công
    public boolean placeBid(Auction auction, BidTransaction bid) {
            boolean isSuccess = placeBidInternal(auction, bid, false);
            if (isSuccess) {
                AuctionHandler.getInstance().handleAutoBid(auction.getId());
            }
            return isSuccess;
    }

    // Logic cốt lõi ghi vào DB
    public boolean placeAutoBid(Auction auction, BidTransaction bid) {
        return placeBidInternal(auction, bid, true);
    }

    private boolean placeBidInternal(Auction auction, BidTransaction bid, boolean allowHighestBidder)
            throws BidRejectedException {
        LocalDateTime now = LocalDateTime.now();
        if (auction.getStatus() == StatusOfAuction.CANCELLED) {
            throw new BidRejectedException("BID_AUCTION_CANCELLED", "Phiên đấu giá đã bị hủy!");
        }
        if (auction.getStatus() == StatusOfAuction.DEAL_CANCELLED) {
            throw new BidRejectedException("BID_DEAL_CANCELLED", "Giao dịch của phiên đấu giá này đã bị hủy!");
        }
        if (now.isBefore(auction.getStartTime())) {
            throw new BidRejectedException("BID_AUCTION_NOT_STARTED", "Phiên đấu giá chưa bắt đầu!");
        }
        if (now.isAfter(auction.getEndTime())) {
            throw new BidRejectedException("BID_AUCTION_ENDED", "Phiên đấu giá đã kết thúc!");
        }
        if (auctionDAO.getBidderReputation(bid.getBidder().getId()) <= 0) {
            throw new BidRejectedException(
                    "BIDDER_REPUTATION_ZERO",
                    "Uy tín của bạn đã về 0, bạn không thể tham gia đấu giá.");
        }
        
        if (!allowHighestBidder && bid.getBidder().getId() == auction.getHighestBidderId()) {
            throw new BidRejectedException(
                    "BIDDER_ALREADY_HIGHEST",
                    "Bạn đang là người dẫn đầu, không cần đặt thêm nhé!");
        }

        double currentHighest = auction.getItem().getCurHighest();
        double requiredMinBid = currentHighest + auction.getBidStep();
        if (bid.getAmount() < requiredMinBid) {
            throw new BidRejectedException(
                    "BID_AMOUNT_TOO_LOW",
                    "Số tiền trả giá phải lớn hơn hoặc bằng "
                            + String.format("%,.0f VNĐ", requiredMinBid)
                            + " (giá hiện tại + bước giá "
                            + String.format("%,.0f VNĐ", auction.getBidStep()) + ").");
        }

        boolean isSuccess = auctionDAO.updateHighestBid(auction.getId(), bid.getBidder().getId(), bid.getAmount());
        
        if (isSuccess) {
            auctionDAO.saveBidTransaction(bid, auction.getId());
            log.info("[OOP printInfo] Accepted bid detail: {}", bid.printInfo());
            
            auction.getItem().setCurHighest(bid.getAmount());
            auction.setHighestBidder(bid.getBidder());
            log.info("[OOP printInfo] Auction after bid: {}", auction.printInfo());

            // Chống Snipe
            LocalDateTime realEndTime = auctionDAO.getEndTime(auction.getId());
            if (realEndTime != null) {
                long secondsLeft = ChronoUnit.SECONDS.between(now, realEndTime);
                if (secondsLeft > 0 && secondsLeft <= SNIPE_THRESHOLD_SECONDS) {
                    boolean extended = auctionDAO.extendAuctionTime(auction.getId(), EXTENSION_MINUTES);
                    if (extended) {
                        LocalDateTime extendedEndTime = auctionDAO.getEndTime(auction.getId());
                        if (extendedEndTime != null) {
                            auction.setEndTime(extendedEndTime);
                        }
                    }
                }
            }
            return true;
        }
        return false;
    }

    private StatusOfAuction getRealAuctionStatus(Auction auction) {
        StatusOfAuction dbStatus = auction.getStatus();
        if (dbStatus == StatusOfAuction.PAID
                || dbStatus == StatusOfAuction.CANCELLED
                || dbStatus == StatusOfAuction.DEAL_CANCELLED
                || dbStatus == StatusOfAuction.FINISHED) {
            return dbStatus;
        }

        LocalDateTime now = LocalDateTime.now();
        if (auction.getStartTime() != null && now.isBefore(auction.getStartTime())) {
            return StatusOfAuction.OPEN;
        }
        if (auction.getEndTime() != null && !now.isBefore(auction.getEndTime())) {
            return StatusOfAuction.FINISHED;
        }
        return StatusOfAuction.RUNNING;
    }
}
